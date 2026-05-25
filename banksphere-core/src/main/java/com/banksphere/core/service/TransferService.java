package com.banksphere.core.service;

import com.banksphere.core.entity.Account;
import com.banksphere.core.entity.Transfer;
import com.banksphere.core.repository.TransferRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

/**
 * Servicio de negocio altamente crítico que gestiona la transaccionalidad
 * y lógica de compensación de transferencias interbancarias en BankSphere.
 */
@Service
public class TransferService {

    private final TransferRepository transferRepository;
    private final AccountService accountService;

    @Autowired
    public TransferService(TransferRepository transferRepository, AccountService accountService) {
        this.transferRepository = transferRepository;
        this.accountService = accountService;
    }

    /**
     * Recupera el historial completo de movimientos asociados a una cuenta.
     */
    @Transactional(readOnly = true)
    public List<Transfer> getMovementsByAccountId(Long accountId) {
        return transferRepository.findAllMovementsByAccountId(accountId);
    }

    /**
     * Procesa y ejecuta una transferencia monetaria de forma estrictamente transaccional.
     *
     * @param sourceIban      IBAN de la cuenta emisora.
     * @param destinationIban IBAN de la cuenta beneficiaria.
     * @param amount          Importe neto a enviar.
     * @param type            Canal de pago (SEPA, SWIFT, INSTANT).
     * @param concept         Concepto o descripción del pago.
     * @return La entidad {@link Transfer} persistida con estado COMPLETED.
     */
    @Transactional
    public Transfer executeTransfer(String sourceIban, String destinationIban, BigDecimal amount, Transfer.TransferType type, String concept) {

        // 1. Validaciones de entrada
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("El importe de la transferencia debe ser mayor a cero.");
        }

        if (sourceIban.trim().equalsIgnoreCase(destinationIban.trim())) {
            throw new IllegalArgumentException("No está permitido realizar transferencias hacia la misma cuenta de origen.");
        }

        // 2. Recuperación y validación de existencia de cuentas
        Account source = accountService.getAccountByIban(sourceIban);
        Account destination = accountService.getAccountByIban(destinationIban);

        // 3. Validación de estados de cuentas bancarias
        if (source.getStatus() != Account.AccountStatus.ACTIVE) {
            throw new IllegalStateException("La transferencia ha sido rechazada: La cuenta de origen está SUSPENDIDA.");
        }

        if (destination.getStatus() != Account.AccountStatus.ACTIVE) {
            throw new IllegalStateException("La transferencia ha sido rechazada: La cuenta de destino está bloqueada o SUSPENDIDA.");
        }

        // 4. Cálculo automático de comisiones bancarias
        BigDecimal fee = calculateFee(amount, type);
        BigDecimal totalCost = amount.add(fee);

        // 5. Validación de fondos suficientes en cuenta emisora
        if (source.getBalance().compareTo(totalCost) < 0) {
            throw new RuntimeException("Fondos insuficientes en la cuenta de origen. Saldo disponible: "
                    + source.getBalance() + " " + source.getCurrency()
                    + ". Coste total requerido (importe + comisión): " + totalCost + " " + source.getCurrency());
        }

        // 6. Conversión de divisas si operan en monedas diferentes (EUR <-> USD)
        BigDecimal amountToDeposit = amount;
        if (source.getCurrency() != destination.getCurrency()) {
            BigDecimal exchangeRate = getMockExchangeRate(source.getCurrency(), destination.getCurrency());
            amountToDeposit = amount.multiply(exchangeRate).setScale(2, RoundingMode.HALF_UP);
        }

        // 7. Modificación atómica de balances
        source.setBalance(source.getBalance().subtract(totalCost));
        destination.setBalance(destination.getBalance().add(amountToDeposit));

        // Salvamos los nuevos balances en la base de datos
        accountService.updateAccount(source);
        accountService.updateAccount(destination);

        // 8. Registro y persistencia del recibo de la transferencia
        Transfer transfer = Transfer.builder()
                .sourceAccount(source)
                .destinationAccount(destination)
                .amount(amount)
                .fee(fee)
                .concept(concept != null && !concept.trim().isEmpty() ? concept.trim() : "Transferencia " + type)
                .transferType(type)
                .status(Transfer.TransferStatus.COMPLETED) // Liquidada con éxito
                .build();

        Transfer savedTransfer = transferRepository.save(transfer);

        // [Módulo Eventos - RabbitMQ]
        // TODO: Publicar savedTransfer en la cola de RabbitMQ 'transfer.queue'
        // para que sea analizada asíncronamente por el motor Antifraude y procesada por notificaciones.

        return savedTransfer;
    }

    /**
     * Lógica de Negocio: Calcula la comisión según el canal de transferencia.
     * - SEPA: 0% comisión (gratis)
     * - INSTANT: Comisión fija de 2.50 unidades
     * - SWIFT: Comisión porcentual de 1.5% del importe enviado
     */
    private BigDecimal calculateFee(BigDecimal amount, Transfer.TransferType type) {
        switch (type) {
            case INSTANT:
                return new BigDecimal("2.50"); // Tarifa plana express
            case SWIFT:
                // 1.5% del importe enviado
                BigDecimal percentage = new BigDecimal("0.015");
                return amount.multiply(percentage).setScale(2, RoundingMode.HALF_UP);
            case SEPA:
            default:
                return BigDecimal.ZERO;
        }
    }

    /**
     * Retorna una tasa de cambio simulada en tiempo real.
     * EUR a USD = 1.10 (El dólar vale un 10% menos)
     * USD a EUR = 0.90 (El euro vale un 10% más)
     */
    private BigDecimal getMockExchangeRate(Account.Currency from, Account.Currency to) {
        if (from == Account.Currency.EUR && to == Account.Currency.USD) {
            return new BigDecimal("1.10");
        } else if (from == Account.Currency.USD && to == Account.Currency.EUR) {
            return new BigDecimal("0.90");
        }
        return BigDecimal.ONE;
    }
}
