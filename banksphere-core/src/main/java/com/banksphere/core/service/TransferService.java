package com.banksphere.core.service;

import com.banksphere.core.config.RabbitMQConfig;
import com.banksphere.core.entity.Account;
import com.banksphere.core.entity.Transfer;
import com.banksphere.core.repository.TransferRepository;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Servicio de negocio transaccional para la gestión de transferencias.
 * Integra transaccionalidad bancaria y publicación de eventos distribuidos en RabbitMQ.
 */
@Service
public class TransferService {

    private final TransferRepository transferRepository;
    private final AccountService accountService;
    private final RabbitTemplate rabbitTemplate; // Inyección de la plantilla AMQP

    @Autowired
    public TransferService(TransferRepository transferRepository,
                           AccountService accountService,
                           RabbitTemplate rabbitTemplate) {
        this.transferRepository = transferRepository;
        this.accountService = accountService;
        this.rabbitTemplate = rabbitTemplate;
    }

    /**
     * DTO Plano (Payload) que viaja a través de la cola de RabbitMQ.
     * Evita problemas de serialización proxy de Hibernate y dependencias circulares.
     */
    public record TransferEvent(
            Long id,
            String sourceIban,
            String sourceUserEmail,
            String destinationIban,
            String destinationUserEmail,
            BigDecimal amount,
            BigDecimal fee,
            String concept,
            String transferType,
            String createdAt
    ) {}

    /**
     * Recupera el historial completo de movimientos asociados a una cuenta.
     */
    @Transactional(readOnly = true)
    public List<Transfer> getMovementsByAccountId(Long accountId) {
        return transferRepository.findAllMovementsByAccountId(accountId);
    }

    /**
     * Procesa, ejecuta una transferencia bancaria de forma atómica y publica el evento en RabbitMQ.
     */
    @Transactional
    public Transfer executeTransfer(String sourceIban, String destinationIban, BigDecimal amount, Transfer.TransferType type, String concept) {

        // 1. Validaciones básicas de entrada
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("El importe de la transferencia debe ser mayor a cero.");
        }

        if (sourceIban.trim().equalsIgnoreCase(destinationIban.trim())) {
            throw new IllegalArgumentException("No está permitido realizar transferencias hacia la misma cuenta de origen.");
        }

        // 2. Recuperación y validación de existencia de cuentas
        Account source = accountService.getAccountByIban(sourceIban);
        Account destination = accountService.getAccountByIban(destinationIban);

        // 3. Validación de estados activos (KYC / AML básico)
        if (source.getStatus() != Account.AccountStatus.ACTIVE) {
            throw new IllegalStateException("Transferencia rechazada: La cuenta de origen está SUSPENDIDA.");
        }

        if (destination.getStatus() != Account.AccountStatus.ACTIVE) {
            throw new IllegalStateException("Transferencia rechazada: La cuenta de destino está SUSPENDIDA.");
        }

        // 4. Cálculo automático de comisiones bancarias
        BigDecimal fee = calculateFee(amount, type);
        BigDecimal totalCost = amount.add(fee);

        // 5. Validación de fondos suficientes en cuenta emisora
        if (source.getBalance().compareTo(totalCost) < 0) {
            throw new RuntimeException("Fondos insuficientes en la cuenta de origen. Saldo disponible: "
                    + source.getBalance() + " " + source.getCurrency()
                    + ". Coste total requerido: " + totalCost);
        }

        // 6. Conversión de divisas si operan en monedas diferentes (EUR <-> USD)
        BigDecimal amountToDeposit = amount;
        if (source.getCurrency() != destination.getCurrency()) {
            BigDecimal exchangeRate = getMockExchangeRate(source.getCurrency(), destination.getCurrency());
            amountToDeposit = amount.multiply(exchangeRate).setScale(2, RoundingMode.HALF_UP);
        }

        // 7. Modificación atómica de balances en base de datos
        source.setBalance(source.getBalance().subtract(totalCost));
        destination.setBalance(destination.getBalance().add(amountToDeposit));

        accountService.updateAccount(source);
        accountService.updateAccount(destination);

        // 8. Registro y persistencia de la transferencia en Postgres
        Transfer transfer = Transfer.builder()
                .sourceAccount(source)
                .destinationAccount(destination)
                .amount(amount)
                .fee(fee)
                .concept(concept != null && !concept.trim().isEmpty() ? concept.trim() : "Transferencia " + type)
                .transferType(type)
                .status(Transfer.TransferStatus.COMPLETED)
                .build();

        Transfer savedTransfer = transferRepository.save(transfer);

        // 9. PUBLICACIÓN DE EVENTO EN RABBITMQ
        // Creamos el Payload plano y ligero para la cola
        try {
            TransferEvent event = new TransferEvent(
                    savedTransfer.getId(),
                    source.getIban(),
                    source.getUser().getEmail(),
                    destination.getIban(),
                    destination.getUser().getEmail(),
                    savedTransfer.getAmount(),
                    savedTransfer.getFee(),
                    savedTransfer.getConcept(),
                    savedTransfer.getTransferType().name(),
                    savedTransfer.getCreatedAt().toString()
            );

            // Publicamos asíncronamente en el Broker
            rabbitTemplate.convertAndSend(
                    RabbitMQConfig.EXCHANGE_NAME,
                    RabbitMQConfig.TRANSFER_ROUTING_KEY,
                    event
            );

            System.out.println(">>> [Event-Publisher] Evento de transferencia enviado a RabbitMQ con éxito. ID: " + savedTransfer.getId());
        } catch (Exception e) {
            // Un fallo en el broker de mensajería no debe tirar abajo la transacción bancaria de Postgres.
            // Registramos el error de forma segura en logs para reintentos o análisis.
            System.err.println(">>> [Event-Publisher-Error] Fallo al publicar el evento en el broker: " + e.getMessage());
        }

        return savedTransfer;
    }

    private BigDecimal calculateFee(BigDecimal amount, Transfer.TransferType type) {
        switch (type) {
            case INSTANT:
                return new BigDecimal("2.50");
            case SWIFT:
                BigDecimal percentage = new BigDecimal("0.015");
                return amount.multiply(percentage).setScale(2, RoundingMode.HALF_UP);
            case SEPA:
            default:
                return BigDecimal.ZERO;
        }
    }

    private BigDecimal getMockExchangeRate(Account.Currency from, Account.Currency to) {
        if (from == Account.Currency.EUR && to == Account.Currency.USD) {
            return new BigDecimal("1.10");
        } else if (from == Account.Currency.USD && to == Account.Currency.EUR) {
            return new BigDecimal("0.90");
        }
        return BigDecimal.ONE;
    }
}