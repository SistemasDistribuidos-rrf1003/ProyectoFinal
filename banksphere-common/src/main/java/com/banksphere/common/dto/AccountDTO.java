package com.banksphere.common.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AccountDTO {
    private Long id;
    private String iban;

    @NotBlank(message = "El tipo de cuenta es obligatorio")
    private String accountType;

    @NotNull(message = "El saldo no puede ser nulo")
    @PositiveOrZero(message = "El saldo inicial no puede ser negativo")
    private BigDecimal balance;

    @NotBlank(message = "La divisa es obligatoria (EUR/USD)")
    private String currency;

    private String status;

    @NotNull(message = "El ID del titular/usuario es obligatorio")
    private Long userId;

    private LocalDateTime createdAt;
}