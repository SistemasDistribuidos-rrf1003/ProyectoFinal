package com.banksphere.core.controller;

import com.banksphere.common.dto.TransferDTO;
import com.banksphere.core.entity.Account;
import com.banksphere.core.entity.User;
import com.banksphere.core.repository.AccountRepository;
import com.banksphere.core.repository.UserRepository;
import com.banksphere.core.service.TransferService.TransferEvent;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;

@SpringBootTest
@AutoConfigureMockMvc(addFilters = false) // Desactiva filtros de login de Spring Security para el test de integración API
@ActiveProfiles("test") // Activa el perfil de base de datos H2 de 'application-test.yml'
@Transactional // Revierte cualquier insert en H2 después de cada test
public class TransferControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private AccountRepository accountRepository;

    @Autowired
    private ObjectMapper objectMapper; // Convertidor de JSON de Spring

    @MockBean
    private RabbitTemplate rabbitTemplate; // Mockeamos el Broker de mensajes

    private Account sourceAccount;
    private Account destinationAccount;

    @BeforeEach
    void setUp() {
        // 1. Insertamos un usuario semilla de prueba en H2
        User client = User.builder()
                .firstName("Javier")
                .lastName("Gomez")
                .email("test.integration@banksphere.com")
                .nationalId("12345678I")
                .password("password_enc")
                .role(User.UserRole.USER)
                .status(User.UserStatus.ACTIVE)
                .build();
        userRepository.save(client);

        // 2. Insertamos la cuenta origen con saldo
        sourceAccount = Account.builder()
                .iban("ES0112345678901234567890")
                .accountType(Account.AccountType.CHECKING)
                .balance(new BigDecimal("1000.00"))
                .currency(Account.Currency.EUR)
                .status(Account.AccountStatus.ACTIVE)
                .user(client)
                .build();
        accountRepository.save(sourceAccount);

        // 3. Insertamos la cuenta de destino
        destinationAccount = Account.builder()
                .iban("ES9909876543210987654321")
                .accountType(Account.AccountType.SAVINGS)
                .balance(new BigDecimal("500.00"))
                .currency(Account.Currency.EUR)
                .status(Account.AccountStatus.ACTIVE)
                .user(client)
                .build();
        accountRepository.save(destinationAccount);
    }

    @Test
    @DisplayName("Debería procesar la transferencia, restar saldos en H2 y publicar evento en RabbitMQ")
    void postExecuteTransfer_Integration_Success() throws Exception {
        // Arrange - Creamos el DTO de transferencia
        TransferDTO transferRequest = TransferDTO.builder()
                .sourceIban(sourceAccount.getIban())
                .destinationIban(destinationAccount.getIban())
                .amount(new BigDecimal("300.00"))
                .transferType("SEPA")
                .concept("Alquiler Junio")
                .build();

        // Act - Realizamos la petición HTTP POST a la REST API
        mockMvc.perform(post("/api/v1/openbanking/transfers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(transferRequest)))

                // Assert - Comprobaciones
                .andExpect(status().isCreated()) // Esperamos HTTP 201 Created
                .andExpect(jsonPath("$.status").value("COMPLETED"))
                .andExpect(jsonPath("$.amount").value(300.00))
                .andExpect(jsonPath("$.fee").value(0.00));

        // 1. Validación de Base de Datos H2: Comprobamos que las entidades de base de datos se alteraron
        Account updatedSource = accountRepository.findByIban(sourceAccount.getIban()).orElseThrow();
        Account updatedDest = accountRepository.findByIban(destinationAccount.getIban()).orElseThrow();

        assertEquals(new BigDecimal("700.00"), updatedSource.getBalance()); // 1000 - 300
        assertEquals(new BigDecimal("800.00"), updatedDest.getBalance());   // 500 + 300

        // 2. Validación de Sistemas Distribuidos: Comprobamos que el evento JSON se envió a RabbitMQ
        verify(rabbitTemplate, times(1)).convertAndSend(
                anyString(),
                anyString(),
                any(TransferEvent.class)
        );
    }

    @Test
    @DisplayName("Debería rechazar con HTTP 400 si el importe de la transferencia es negativo")
    void postExecuteTransfer_NegativeAmount_ReturnsBadRequest() throws Exception {
        // Arrange
        TransferDTO badRequest = TransferDTO.builder()
                .sourceIban(sourceAccount.getIban())
                .destinationIban(destinationAccount.getIban())
                .amount(new BigDecimal("-50.00")) // Importe erróneo
                .transferType("SEPA")
                .build();

        // Act & Assert
        mockMvc.perform(post("/api/v1/openbanking/transfers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(badRequest)))
                .andExpect(status().isBadRequest()); // Esperamos HTTP 400 Bad Request
    }
}