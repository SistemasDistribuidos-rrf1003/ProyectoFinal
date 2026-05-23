package com.banksphere.core.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

/**
 * Entidad de persistencia JPA que representa a un usuario/cliente en la plataforma BankSphere.
 * Incluye datos personales, credenciales cifradas, roles y estado del usuario.
 */
@Entity
@Table(name = "users")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "first_name", nullable = false, length = 50)
    private String firstName;

    @Column(name = "last_name", nullable = false, length = 50)
    private String lastName;

    @Column(name = "email", unique = true, nullable = false, length = 100)
    private String email;

    @Column(name = "phone", length = 20)
    private String phone;

    @Column(name = "national_id", unique = true, nullable = false, length = 30)
    private String nationalId;

    @Column(name = "password", nullable = false, length = 100)
    private String password;

    @Enumerated(EnumType.STRING)
    @Column(name = "role", nullable = false, length = 20)
    private UserRole role;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private UserStatus status;

    // Relación bidireccional Uno a Uno con los detalles de KYC/AML
    @OneToOne(mappedBy = "user", cascade = CascadeType.ALL, fetch = FetchType.LAZY, orphanRemoval = true)
    private KycDetail kycDetail;

    /**
     * Inicializa automáticamente la fecha de registro antes de persistir en base de datos.
     */
    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
        if (this.status == null) {
            this.status = UserStatus.PENDING_KYC; // Por defecto al registrarse
        }
    }

    /**
     * Enums asociados al Usuario
     */
    public enum UserRole {
        ADMIN,
        USER,
        ANALYST
    }

    public enum UserStatus {
        ACTIVE,
        SUSPENDED,
        PENDING_KYC
    }
}
