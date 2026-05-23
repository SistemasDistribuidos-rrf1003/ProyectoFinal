package com.banksphere.core.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

/**
 * Entidad de persistencia JPA que almacena la información detallada del cumplimiento regulatorio
 * KYC (Know Your Customer) y AML (Anti-Money Laundering) de cada usuario en BankSphere.
 */
@Entity
@Table(name = "kyc_details")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class KycDetail {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Relación Uno a Uno estricta con el Usuario (Propietaria de la clave foránea user_id)
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", referencedColumnName = "id", nullable = false, unique = true)
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(name = "verification_status", nullable = false, length = 30)
    private KycStatus verificationStatus;

    @Enumerated(EnumType.STRING)
    @Column(name = "risk_level", nullable = false, length = 20)
    private RiskLevel riskLevel;

    @Column(name = "document_type", length = 30)
    private String documentType;

    @Column(name = "document_url", length = 255)
    private String documentUrl;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    /**
     * Inicializa las fechas antes de persistir o actualizar el registro.
     */
    @PrePersist
    protected void onCreate() {
        this.updatedAt = LocalDateTime.now();
        if (this.verificationStatus == null) {
            this.verificationStatus = KycStatus.PENDING;
        }
        if (this.riskLevel == null) {
            this.riskLevel = RiskLevel.MEDIUM; // Riesgo moderado por defecto hasta evaluación
        }
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

    /**
     * Enums asociados al módulo KYC/AML
     */
    public enum KycStatus {
        VERIFIED,
        REJECTED,
        PENDING,
        IN_PROGRESS
    }

    public enum RiskLevel {
        LOW,
        MEDIUM,
        HIGH
    }
}
