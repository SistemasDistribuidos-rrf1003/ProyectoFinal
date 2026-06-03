package com.banksphere.antifraud;

import com.banksphere.antifraud.consumer.TransactionConsumer.TransferEvent;
import com.banksphere.antifraud.engine.*;
import com.banksphere.antifraud.engine.RiskRulesEngine.RiskEvaluation;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;

public class RiskRulesEngineTest {

    private RiskRulesEngine riskRulesEngine;

    @BeforeEach
    void setUp() {
        riskRulesEngine = new RiskRulesEngine();
    }

    @Test
    @DisplayName("Debería evaluar como riesgo bajo (LOW) una transferencia SEPA estándar de 500€")
    void evaluateTransaction_LowRisk_Success() {
        // Arrange
        TransferEvent event = new TransferEvent(
                1L,
                "ES2114650100991234567890",
                "client@banksphere.com",
                "ES9814650100990987654321",
                "receiver@banksphere.com",
                new BigDecimal("500.00"),
                BigDecimal.ZERO,
                "Pago cena",
                "SEPA",
                "2026-06-03T18:32:00"
        );

        // Act
        RiskEvaluation evaluation = riskRulesEngine.evaluateTransaction(event);

        // Assert
        assertNotNull(evaluation);
        assertFalse(evaluation.isSuspicious());
        assertEquals("LOW", evaluation.getRiskLevel());
        assertEquals(0, evaluation.getRiskScore());
        assertEquals("APPROVE", evaluation.getActionRecommended());
        assertTrue(evaluation.getTriggeredRules().isEmpty());
    }

    @Test
    @DisplayName("Debería clasificar como HIGH y recomendar REVIEW si el importe supera 10.000€ (Regla AML)")
    void evaluateTransaction_AmlThresholdExceeded_TriggersRule() {
        // Arrange
        TransferEvent event = new TransferEvent(
                2L,
                "ES2114650100991234567890",
                "client@banksphere.com",
                "ES9814650100990987654321",
                "receiver@banksphere.com",
                new BigDecimal("12000.00"), // Mayor de 10.000
                BigDecimal.ZERO,
                "Compra de vehículo",
                "SEPA",
                "2026-06-03T18:32:00"
        );

        // Act
        RiskEvaluation evaluation = riskRulesEngine.evaluateTransaction(event);

        // Assert
        assertTrue(evaluation.isSuspicious());
        assertEquals("HIGH", evaluation.getRiskLevel());
        assertEquals(40, evaluation.getRiskScore());
        assertEquals("REVIEW", evaluation.getActionRecommended());
        assertTrue(evaluation.getTriggeredRules().get(0).contains("RULE_AML_THRESHOLD_EXCEEDED"));
    }

    @Test
    @DisplayName("Debería clasificar como CRITICAL y recomendar BLOCK si se realizan 3 transferencias en menos de un minuto")
    void evaluateTransaction_HighFrequency_TriggersBlock() {
        // Arrange - Simulamos 3 transferencias consecutivas del mismo emisor
        String sourceIban = "ES2114650100991234567890";

        TransferEvent event1 = createEvent(3L, sourceIban, new BigDecimal("100.00"), "SEPA");
        TransferEvent event2 = createEvent(4L, sourceIban, new BigDecimal("100.00"), "SEPA");
        TransferEvent event3 = createEvent(5L, sourceIban, new BigDecimal("100.00"), "SEPA");

        // Act
        riskRulesEngine.evaluateTransaction(event1); // 1ª operación
        riskRulesEngine.evaluateTransaction(event2); // 2ª operación
        RiskEvaluation finalEvaluation = riskRulesEngine.evaluateTransaction(event3); // 3ª operación (gatillo)

        // Assert
        assertTrue(finalEvaluation.isSuspicious());
        assertEquals("CRITICAL", finalEvaluation.getRiskLevel());
        assertEquals(60, finalEvaluation.getRiskScore()); // Suma 60 por frecuencia
        assertEquals("BLOCK", finalEvaluation.getActionRecommended());
        assertTrue(finalEvaluation.getTriggeredRules().stream()
                .anyMatch(rule -> rule.contains("RULE_HIGH_FREQUENCY_FRAUD")));
    }

    // Helper method to generate events
    private TransferEvent createEvent(Long id, String sourceIban, BigDecimal amount, String type) {
        return new TransferEvent(
                id,
                sourceIban,
                "client@banksphere.com",
                "ES9814650100990987654321",
                "receiver@banksphere.com",
                amount,
                BigDecimal.ZERO,
                "Concepto " + id,
                type,
                "2026-06-03T18:32:00"
        );
    }
}
