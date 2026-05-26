package com.banksphere.antifraud.repository;

import com.banksphere.antifraud.model.FraudAlert;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Repositorio Spring Data JPA para la gestión y consulta de alertas de seguridad.
 */
@Repository
public interface FraudAlertRepository extends JpaRepository<FraudAlert, Long> {

    /**
     * Recupera la lista de alertas filtrando por su nivel de criticidad (LOW, MEDIUM, HIGH, CRITICAL).
     */
    List<FraudAlert> findByRiskLevel(FraudAlert.RiskLevel riskLevel);

    /**
     * Recupera las alertas según su estado de auditoría (ej. pendientes de revisión).
     */
    List<FraudAlert> findByStatus(FraudAlert.AlertStatus status);
}