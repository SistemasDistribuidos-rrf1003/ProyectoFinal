package com.banksphere.common.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TransferDTO {
    private Long id;

    @NotBlank(message = "El IBAN de origen es obligatorio")
    private String sourceIban;

    @NotBlank(message = "El IBAN de destino es obligatorio")
    private String destinationIban;

    @NotNull(message = "El importe es obligatorio")
    @Positive(message = "El importe debe ser mayor que cero")
    private BigDecimal amount;

    private BigDecimal fee;
    private String concept;

    @NotBlank(message = "El tipo de transferencia es obligatorio (SEPA/SWIFT/INSTANT)")
    private String transferType;

    private String status;
    private LocalDateTime createdAt;
}