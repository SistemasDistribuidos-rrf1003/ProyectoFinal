package com.banksphere.common.dto;

import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FraudAlertDTO {
    private Long id;
    private Long transferId;
    private String sourceIban;
    private String destinationIban;
    private BigDecimal amount;
    private Integer riskScore;
    private String riskLevel;
    private String triggeredRules;
    private String status;
    private LocalDateTime createdAt;
}