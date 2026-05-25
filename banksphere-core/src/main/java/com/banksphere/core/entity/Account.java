package com.banksphere.core.entity;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Random;

/**
 * Entidad de persistencia JPA que representa una cuenta bancaria (Account) en la plataforma BankSphere.
 * Incluye validaciones monetarias, control de divisas (EUR/USD) y generación automática de IBAN.
 */
@Entity
@Table(name = "accounts")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Account {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "iban", unique = true, nullable = false, length = 34)
    private String iban;

    @Enumerated(EnumType.STRING)
    @Column(name = "account_type", nullable = false, length = 20)
    private AccountType accountType;

    @Column(name = "balance", nullable = false, precision = 15, scale = 2)
    private BigDecimal balance;

    @Enumerated(EnumType.STRING)
    @Column(name = "currency", nullable = false, length = 3)
    private Currency currency;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private AccountStatus status;

    // Relación ManyToOne con User (Varias cuentas pertenecen a un único cliente)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    /**
     * Ciclo de vida JPA: Antes de insertar una cuenta en la Base de Datos,
     * inicializa la fecha, establece valores iniciales y genera un IBAN aleatorio de formato realista.
     */
    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();

        if (this.status == null) {
            this.status = AccountStatus.ACTIVE;
        }

        if (this.balance == null) {
            this.balance = BigDecimal.ZERO;
        }

        if (this.iban == null || this.iban.trim().isEmpty()) {
            this.iban = generateSpanishIban();
        }
    }

    /**
     * Generador automático de IBAN para España (ES).
     * Estructura estándar del IBAN español (24 caracteres):
     * [ES] + [2 dígitos control] + [4 dígitos banco (1465)] + [4 dígitos sucursal (0100)] + [2 dígitos control (99)] + [10 dígitos de cuenta]
     */
    private String generateSpanishIban() {
        Random random = new Random();

        // Genera 2 dígitos de control del IBAN (ej: entre 10 y 99)
        int controlDigits = random.nextInt(90) + 10;

        // Código de banco simulado de BankSphere: 1465
        String bankCode = "1465";

        // Sucursal fija de la sede central: 0100
        String branchCode = "0100";

        // Dígitos de control del número de cuenta: 99
        String controlAccount = "99";

        StringBuilder accountNumber = new StringBuilder();
        for (int i = 0; i < 10; i++) {
            accountNumber.append(random.nextInt(10));
        }

        // ES + Control + Banco + Sucursal + ControlCuenta + NumeroCuenta
        return String.format("ES%d%s%s%s%s",
                controlDigits,
                bankCode,
                branchCode,
                controlAccount,
                accountNumber.toString()
        );
    }

    /**
     * Enums asociados a las Cuentas Bancarias
     */
    public enum AccountType {
        SAVINGS,   // Cuenta Ahorros
        CHECKING,  // Cuenta Corriente
        BUSINESS   // Cuenta de Empresas / Negocios
    }

    public enum Currency {
        EUR,  // Euros
        USD   // Dólares Americanos
    }

    public enum AccountStatus {
        ACTIVE,
        SUSPENDED
    }
}
