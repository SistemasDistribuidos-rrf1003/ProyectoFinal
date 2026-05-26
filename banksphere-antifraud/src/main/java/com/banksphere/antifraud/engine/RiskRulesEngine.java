package com.banksphere.antifraud.engine;

import com.banksphere.antifraud.consumer.TransactionConsumer.TransferEvent;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * Motor Inteligente de Reglas de Riesgo de BankSphere.
 * Aplica reglas heurísticas de prevención de fraude y AML de manera ultra-rápida.
 */
@Component
public class RiskRulesEngine {

    // Límite legal europeo de blanqueo de capitales (10,000.00 EUR/USD)
    private static final BigDecimal AML_THRESHOLD = new BigDecimal("10000.00");

    // Estructura Concurrente en memoria para rastrear ráfagas de operaciones por IBAN de origen
    // Simula de forma eficiente y limpia la caché distribuida de Redis
    private final Map<String, List<LocalDateTime>> transactionTimestampsHistory = new ConcurrentHashMap<>();

    /**
     * DTO interno para estructurar el resultado final del análisis de fraude.
     */
    @lombok.Getter
    @lombok.ToString
    @lombok.Builder
    public static class RiskEvaluation {
        private final boolean suspicious;
        private final int riskScore; // Escala de 0 a 100
        private final String riskLevel; // LOW, MEDIUM, HIGH, CRITICAL
        private final List<String> triggeredRules;
        private final String actionRecommended; // APPROVE, REVIEW, BLOCK
    }

    /**
     * Evalúa una transacción entrante aplicando el motor de reglas heurísticas.
     */
    public RiskEvaluation evaluateTransaction(TransferEvent event) {
        List<String> rulesViolated = new ArrayList<>();
        int scoreAccumulator = 0;

        // =====================================================================
        // REGLA 1: Transferencias superiores a 10.000 (Alerta AML Automática)
        // =====================================================================
        if (event.amount().compareTo(AML_THRESHOLD) > 0) {
            rulesViolated.add("RULE_AML_THRESHOLD_EXCEEDED: Importe superior a 10.000€ (" + event.amount() + ")");
            scoreAccumulator += 40; // Suma 40 puntos de riesgo
        }

        // =====================================================================
        // REGLA 2: Ráfagas de transacciones (Más de 3 en menos de 1 minuto)
        // =====================================================================
        if (checkHighFrequencyFraud(event.sourceIban())) {
            rulesViolated.add("RULE_HIGH_FREQUENCY_FRAUD: Más de 3 operaciones en menos de 60 segundos.");
            scoreAccumulator += 60; // Suma 60 puntos de riesgo (Crítico)
        }

        // =====================================================================
        // REGLA 3: Operación SWIFT Internacional (Fuera de zona SEPA)
        // =====================================================================
        if ("SWIFT".equalsIgnoreCase(event.transferType())) {
            rulesViolated.add("RULE_SWIFT_INTERNATIONAL: Transferencia transfronteriza fuera de la red de protección SEPA.");
            scoreAccumulator += 20; // Suma 20 puntos de riesgo
        }

        // =====================================================================
        // CONSOLIDACIÓN DE RESULTADOS Y GENERACIÓN DEL SCORING
        // =====================================================================
        int finalScore = Math.min(scoreAccumulator, 100); // El score máximo es 100
        String riskLevel = "LOW";
        String action = "APPROVE";
        boolean suspicious = false;

        if (finalScore >= 80) {
            riskLevel = "CRITICAL";
            action = "BLOCK"; // Bloqueo automático preventivo
            suspicious = true;
        } else if (finalScore >= 40) {
            riskLevel = "HIGH";
            action = "REVIEW"; // Requiere auditoría del Analista
            suspicious = true;
        } else if (finalScore >= 20) {
            riskLevel = "MEDIUM";
            action = "REVIEW";
        }

        return RiskEvaluation.builder()
                .suspicious(suspicious)
                .riskScore(finalScore)
                .riskLevel(riskLevel)
                .triggeredRules(rulesViolated)
                .actionRecommended(action)
                .build();
    }

    /**
     * Algoritmo de Ventana Deslizable (Sliding Window Log) en memoria para el control de frecuencias.
     * Mantiene las marcas de tiempo del IBAN origen limpiando los registros antiguos de 1 minuto.
     * Retorna TRUE si se registran 3 o más operaciones en el último minuto.
     */
    private boolean checkHighFrequencyFraud(String sourceIban) {
        LocalDateTime now = LocalDateTime.now();

        // 1. Obtenemos o creamos la lista de marcas de tiempo del emisor de forma segura
        List<LocalDateTime> timestamps = transactionTimestampsHistory.computeIfAbsent(
                sourceIban,
                k -> Collections.synchronizedList(new ArrayList<>())
        );

        synchronized (timestamps) {
            // 2. Limpieza: Eliminamos todas las marcas de tiempo que tengan más de 60 segundos
            timestamps.removeIf(timestamp -> ChronoUnit.SECONDS.between(timestamp, now) > 60);

            // 3. Agregamos la marca de tiempo de la transacción actual
            timestamps.add(now);

            // 4. Verificación de umbral: Si tiene 3 o más transacciones acumuladas en el último minuto
            // (La transacción actual ya está incluida en la lista, por lo que buscamos >= 3)
            return timestamps.size() >= 3;
        }
    }
}