package com.banksphere.antifraud.model;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Entidad de persistencia JPA que representa una Alerta de Fraude o AML en BankSphere.
 * Registra el scoring de riesgo calculado y las reglas del sistema que fueron vulneradas.
 */
@Entity
@Table(name = "fraud_alerts")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FraudAlert {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "transfer_id", nullable = false)
    private Long transferId; // Enlace lógico al ID de transferencia del Core

    @Column(name = "source_iban", nullable = false, length = 34)
    private String sourceIban;

    @Column(name = "destination_iban", nullable = false, length = 34)
    private String destinationIban;

    @Column(name = "amount", nullable = false, precision = 15, scale = 2)
    private BigDecimal amount;

    @Column(name = "risk_score", nullable = false)
    private Integer riskScore; // Escala 0 a 100

    @Enumerated(EnumType.STRING)
    @Column(name = "risk_level", nullable = false, length = 20)
    private RiskLevel riskLevel; // Niveles de criticidad: LOW, MEDIUM, HIGH, CRITICAL

    @Column(name = "triggered_rules", length = 500)
    private String triggeredRules; // Lista de reglas violadas concatenadas por comas

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 30)
    private AlertStatus status; // Estado: PENDING_REVIEW, RESOLVED_GENUINE, RESOLVED_FRAUDULENT

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
        if (this.status == null) {
            this.status = AlertStatus.PENDING_REVIEW;
        }
    }

    /**
     * Niveles de Criticidad de Alerta
     */
    public enum RiskLevel {
        LOW,      // Bajo Riesgo
        MEDIUM,   // Riesgo Medio
        HIGH,     // Alto Riesgo (AML / SWIFT grandes)
        CRITICAL  // Riesgo Crítico (Fraude por ráfaga de transacciones)
    }

    public enum AlertStatus {
        PENDING_REVIEW,       // Pendiente de auditoría por el Analista
        RESOLVED_GENUINE,     // Auditoría cerrada: Transacción legítima
        RESOLVED_FRAUDULENT   // Auditoría cerrada: Fraude confirmado
    }
}