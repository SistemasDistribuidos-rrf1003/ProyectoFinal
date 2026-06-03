package com.banksphere.core.service;

import com.banksphere.core.config.RabbitMQConfig;
import com.banksphere.core.entity.Account;
import com.banksphere.core.entity.Transfer;
import com.banksphere.core.entity.User;
import com.banksphere.core.repository.TransferRepository;
import com.banksphere.core.service.AccountService;
import com.banksphere.core.service.TransferService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.rabbit.core.RabbitTemplate;

import java.math.BigDecimal;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class TransferServiceTest {

    @Mock
    private TransferRepository transferRepository;

    @Mock
    private AccountService accountService;

    @Mock
    private RabbitTemplate rabbitTemplate;

    @InjectMocks
    private TransferService transferService;

    private Account sourceAccount;
    private Account destinationAccount;
    private User client;

    @BeforeEach
    void setUp() {
        client = User.builder().id(1L).email("client@banksphere.com").firstName("Javier").lastName("Gomez").build();

        sourceAccount = Account.builder()
                .id(1L)
                .iban("ES2114650100991234567890")
                .balance(new BigDecimal("1000.00"))
                .currency(Account.Currency.valueOf("EUR"))
                .status(Account.AccountStatus.ACTIVE)
                .user(client)
                .build();

        destinationAccount = Account.builder()
                .id(2L)
                .iban("ES9814650100990987654321")
                .balance(new BigDecimal("500.00"))
                .currency(Account.Currency.valueOf("EUR"))
                .status(Account.AccountStatus.ACTIVE)
                .user(client)
                .build();
    }

    @Test
    @DisplayName("Debería ejecutar con éxito una transferencia SEPA estándar (Sin Comisión)")
    void executeTransfer_Sepa_Success() {
        // Arrange
        String srcIban = sourceAccount.getIban();
        String destIban = destinationAccount.getIban();
        BigDecimal amount = new BigDecimal("200.00");
        String concept = "Pago de cena";

        when(accountService.getAccountByIban(srcIban)).thenReturn(sourceAccount);
        when(accountService.getAccountByIban(destIban)).thenReturn(destinationAccount);

        Transfer mockTransfer = Transfer.builder()
                .id(100L)
                .sourceAccount(sourceAccount)
                .destinationAccount(destinationAccount)
                .amount(amount)
                .fee(BigDecimal.ZERO)
                .concept(concept)
                .transferType(Transfer.TransferType.SEPA)
                .status(Transfer.TransferStatus.COMPLETED)
                .build();

        when(transferRepository.save(any(Transfer.class))).thenReturn(mockTransfer);

        // Act
        Transfer result = transferService.executeTransfer(srcIban, destIban, amount, Transfer.TransferType.SEPA, concept);

        // Assert
        assertNotNull(result);
        assertEquals(Transfer.TransferStatus.COMPLETED, result.getStatus());
        assertEquals(new BigDecimal("800.00"), sourceAccount.getBalance()); // 1000 - 200 (sepa sin fee)
        assertEquals(new BigDecimal("700.00"), destinationAccount.getBalance()); // 500 + 200

        // Verificamos que se actualizan los saldos en la DB
        verify(accountService, times(2)).updateAccount(any(Account.class));
        // Verificamos que se publica el evento en RabbitMQ
        verify(rabbitTemplate, times(1)).convertAndSend(
                eq(RabbitMQConfig.EXCHANGE_NAME),
                eq(RabbitMQConfig.TRANSFER_ROUTING_KEY),
                any(TransferService.TransferEvent.class)
        );
    }

    @Test
    @DisplayName("Debería lanzar excepción si la cuenta de origen tiene fondos insuficientes")
    void executeTransfer_InsufficientFunds_ThrowsException() {
        // Arrange
        String srcIban = sourceAccount.getIban();
        String destIban = destinationAccount.getIban();
        BigDecimal amount = new BigDecimal("1200.00"); // Supera los 1000 de saldo

        when(accountService.getAccountByIban(srcIban)).thenReturn(sourceAccount);
        when(accountService.getAccountByIban(destIban)).thenReturn(destinationAccount);

        // Act & Assert
        RuntimeException exception = assertThrows(RuntimeException.class, () ->
                transferService.executeTransfer(srcIban, destIban, amount, Transfer.TransferType.SEPA, "Fallo")
        );

        assertTrue(exception.getMessage().contains("Fondos insuficientes"));
        verify(transferRepository, never()).save(any());
        verify(rabbitTemplate, never()).convertAndSend(anyString(), anyString(), any(Object.class));
    }

    @Test
    @DisplayName("Debería cobrar una comisión fija de 2.50€ al ser transferencia inmediata (INSTANT)")
    void executeTransfer_Instant_AppliesFlatFee() {
        // Arrange
        String srcIban = sourceAccount.getIban();
        String destIban = destinationAccount.getIban();
        BigDecimal amount = new BigDecimal("100.00");

        when(accountService.getAccountByIban(srcIban)).thenReturn(sourceAccount);
        when(accountService.getAccountByIban(destIban)).thenReturn(destinationAccount);

        Transfer mockTransfer = Transfer.builder()
                .id(101L)
                .amount(amount)
                .fee(new BigDecimal("2.50"))
                .transferType(Transfer.TransferType.INSTANT)
                .build();
        when(transferRepository.save(any(Transfer.class))).thenReturn(mockTransfer);

        // Act
        Transfer result = transferService.executeTransfer(srcIban, destIban, amount, Transfer.TransferType.INSTANT, "Express");

        // Assert
        assertEquals(new BigDecimal("897.50"), sourceAccount.getBalance()); // 1000 - (100 + 2.50 fee)
        assertEquals(new BigDecimal("600.00"), destinationAccount.getBalance()); // 500 + 100 (la comisión no va a la otra cuenta)
    }

    @Test
    @DisplayName("Debería lanzar excepción si la cuenta de origen está SUSPENDIDA")
    void executeTransfer_SourceSuspended_ThrowsException() {
        // Arrange
        sourceAccount.setStatus(Account.AccountStatus.SUSPENDED);
        when(accountService.getAccountByIban(sourceAccount.getIban())).thenReturn(sourceAccount);
        when(accountService.getAccountByIban(destinationAccount.getIban())).thenReturn(destinationAccount);

        // Act & Assert
        assertThrows(IllegalStateException.class, () ->
                transferService.executeTransfer(sourceAccount.getIban(), destinationAccount.getIban(), new BigDecimal("10.00"), Transfer.TransferType.SEPA, "Test")
        );
    }
}