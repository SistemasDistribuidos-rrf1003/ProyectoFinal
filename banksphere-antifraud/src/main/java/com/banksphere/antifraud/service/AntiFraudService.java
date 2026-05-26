package com.banksphere.antifraud.service;

import com.banksphere.antifraud.consumer.TransactionConsumer.TransferEvent;
import com.banksphere.antifraud.engine.RiskRulesEngine;
import com.banksphere.antifraud.engine.RiskRulesEngine.RiskEvaluation;
import com.banksphere.antifraud.model.FraudAlert;
import com.banksphere.antifraud.repository.FraudAlertRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Servicio de negocio encargado de auditar transacciones asíncronas
 * y persistir alertas de fraude según los análisis del motor de reglas.
 */
@Service
public class AntiFraudService {

    private final RiskRulesEngine riskRulesEngine;
    private final FraudAlertRepository fraudAlertRepository;

    @Autowired
    public AntiFraudService(RiskRulesEngine riskRulesEngine, FraudAlertRepository fraudAlertRepository) {
        this.riskRulesEngine = riskRulesEngine;
        this.fraudAlertRepository = fraudAlertRepository;
    }

    /**
     * Recupera todas las alertas de fraude del sistema para auditoría de los analistas.
     */
    @Transactional(readOnly = true)
    public List<FraudAlert> getAllAlerts() {
        return fraudAlertRepository.findAll();
    }

    /**
     * Analiza asíncronamente una transacción entrante.
     * Si el score de riesgo acumulado supera el umbral (20 puntos o más),
     * genera y guarda una Alerta de Fraude en la base de datos.
     */
    @Transactional
    public void analyzeTransaction(TransferEvent event) {
        System.out.println(">>> [AntiFraudService] Iniciando análisis heurístico para Transacción ID: " + event.id());

        // 1. Invocamos al motor inteligente para evaluar la transacción
        RiskEvaluation evaluation = riskRulesEngine.evaluateTransaction(event);

        // 2. Si el puntaje de riesgo es igual o superior a 20 (Riesgo Medio en adelante),
        // registramos y guardamos la alerta de seguridad para los analistas financieros.
        if (evaluation.isSuspicious() || evaluation.getRiskScore() >= 20) {

            // Mapeamos el nivel de riesgo del motor al enum de persistencia
            FraudAlert.RiskLevel riskLevel = FraudAlert.RiskLevel.valueOf(evaluation.getRiskLevel());

            FraudAlert alert = FraudAlert.builder()
                    .transferId(event.id())
                    .sourceIban(event.sourceIban())
                    .destinationIban(event.destinationIban())
                    .amount(event.amount())
                    .riskScore(evaluation.getRiskScore())
                    .riskLevel(riskLevel)
                    .triggeredRules(String.join(", ", evaluation.getTriggeredRules()))
                    .status(FraudAlert.AlertStatus.PENDING_REVIEW)
                    .build();

            // Guardamos la alerta en la Base de Datos
            fraudAlertRepository.save(alert);

            System.out.println(">>> [AntiFraudService] ¡ALERTA DE SEGURIDAD REGISTRADA EN BASE DE DATOS!");
            System.out.println("    [Score de Riesgo]: " + evaluation.getRiskScore() + "/100");
            System.out.println("    [Nivel Criticidad]: " + riskLevel.name() + " (Acción recomendada: " + evaluation.getActionRecommended() + ")");
            System.out.println("    [Reglas Violadas] : " + alert.getTriggeredRules());
        } else {
            // Caso seguro: La transacción no viola reglas y se descarta su registro
            System.out.println(">>> [AntiFraudService] Transacción ID: " + event.id() + " auditada. Evaluación: SEGURA (Riesgo: " + evaluation.getRiskScore() + "/100)");
        }
    }
}