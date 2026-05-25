package com.banksphere.core.entity;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Entidad de persistencia JPA que representa una Transferencia o Transacción monetaria
 * entre dos cuentas bancarias de la plataforma BankSphere.
 */
@Entity
@Table(name = "transfers")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Transfer {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Relación ManyToOne: Cuenta de Origen que emite el capital
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "source_account_id", nullable = false)
    private Account sourceAccount;

    // Relación ManyToOne: Cuenta de Destino que recibe el capital
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "destination_account_id", nullable = false)
    private Account destinationAccount;

    @Column(name = "amount", nullable = false, precision = 15, scale = 2)
    private BigDecimal amount;

    // Comisión bancaria aplicada por el procesamiento (Ej. comisiones SWIFT o Inmediatas)
    @Column(name = "fee", nullable = false, precision = 15, scale = 2)
    private BigDecimal fee;

    @Column(name = "concept", length = 150)
    private String concept;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private TransferStatus status;

    @Enumerated(EnumType.STRING)
    @Column(name = "transfer_type", nullable = false, length = 20)
    private TransferType transferType;

    /**
     * Ciclo de vida JPA: Antes de persistir el registro, inicializa la fecha
     * de realización y establece valores por defecto si no han sido indicados.
     */
    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();

        if (this.status == null) {
            this.status = TransferStatus.PENDING;
        }

        if (this.fee == null) {
            this.fee = BigDecimal.ZERO;
        }
    }

    /**
     * Enums asociados al Módulo de Transferencias
     */
    public enum TransferType {
        SEPA,     // Zona Única de Pagos en Euros (Estándar, comisiones mínimas o nulas)
        SWIFT,    // Sociedad para las Telecomunicaciones Financieras Interbancarias Mundiales (Internacionales con comisión)
        INSTANT   // Pagos inmediatos en tiempo real (Procesamiento express con cargo fijo)
    }

    public enum TransferStatus {
        COMPLETED,        // Transferencia liquidada con éxito
        FAILED,           // Transferencia declinada por fondos insuficientes u otros errores
        PENDING,          // En proceso de liquidación (Ej. colas de mensajería MQ)
        SUSPECTED_FRAUD   // Bloqueada por alertas del motor antifraude (AML)
    }
}