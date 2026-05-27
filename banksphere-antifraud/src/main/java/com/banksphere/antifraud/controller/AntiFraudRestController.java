package com.banksphere.antifraud.controller;

import com.banksphere.antifraud.model.FraudAlert;
import com.banksphere.antifraud.repository.FraudAlertRepository;
import com.banksphere.antifraud.service.AntiFraudService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.OptionalDouble;
import java.util.stream.Collectors;

/**
 * Controlador REST API que expone los endpoints de auditoría y análisis de riesgo
 * para la integración distribuida con el módulo Core de BankSphere.
 */
@RestController
@RequestMapping("/api/v1/antifraud")
public class AntiFraudRestController {

    private final AntiFraudService antiFraudService;
    private final FraudAlertRepository fraudAlertRepository;

    @Autowired
    public AntiFraudRestController(AntiFraudService antiFraudService, FraudAlertRepository fraudAlertRepository) {
        this.antiFraudService = antiFraudService;
        this.fraudAlertRepository = fraudAlertRepository;
    }

    /**
     * DTO de respuesta para representar el Perfil de Riesgo consolidado de una cuenta.
     */
    @lombok.Getter
    @lombok.AllArgsConstructor
    public static class ClientRiskProfile {
        private final String iban;
        private final int totalAlerts;
        private final double averageRiskScore;
        private final int maxRiskScore;
        private final String globalRiskClassification;
        private final boolean recommendSuspension;
    }

    /**
     * Recupera el listado completo de alertas registradas en el sistema.
     * GET /api/v1/antifraud/alerts
     */
    @GetMapping("/alerts")
    public ResponseEntity<List<FraudAlert>> getAllAlerts() {
        return ResponseEntity.ok(antiFraudService.getAllAlerts());
    }

    /**
     * Recupera únicamente las alertas de seguridad pendientes de auditoría.
     * GET /api/v1/antifraud/alerts/pending
     */
    @GetMapping("/alerts/pending")
    public ResponseEntity<List<FraudAlert>> getPendingAlerts() {
        List<FraudAlert> pending = fraudAlertRepository.findByStatus(FraudAlert.AlertStatus.PENDING_REVIEW);
        return ResponseEntity.ok(pending);
    }

    /**
     * Calcula y retorna el Perfil de Riesgo Histórico de una cuenta bancaria por su IBAN.
     * GET /api/v1/antifraud/risk/iban/{iban}
     */
    @GetMapping("/risk/iban/{iban}")
    public ResponseEntity<ClientRiskProfile> getClientRiskProfile(@PathVariable("iban") String iban) {
        // 1. Filtramos todas las alertas históricas asociadas a esta cuenta emisora
        List<FraudAlert> clientAlerts = fraudAlertRepository.findAll().stream()
                .filter(alert -> alert.getSourceIban().equalsIgnoreCase(iban.trim()))
                .collect(Collectors.toList());

        int total = clientAlerts.size();

        // 2. Si no registra alertas, su perfil es de riesgo bajo
        if (total == 0) {
            return ResponseEntity.ok(new ClientRiskProfile(iban, 0, 0.0, 0, "LOW_RISK", false));
        }

        // 3. Calculamos métricas matemáticas de riesgo
        double avgScore = clientAlerts.stream()
                .mapToInt(FraudAlert::getRiskScore)
                .average()
                .orElse(0.0);

        int maxScore = clientAlerts.stream()
                .mapToInt(FraudAlert::getRiskScore)
                .max()
                .orElse(0);

        // 4. Clasificamos semánticamente el nivel global
        String classification = "LOW_RISK";
        boolean recommendSuspension = false;

        if (maxScore >= 80 || avgScore >= 70) {
            classification = "CRITICAL_RISK";
            recommendSuspension = true; // Se recomienda congelar la cuenta inmediatamente
        } else if (maxScore >= 40 || avgScore >= 40) {
            classification = "HIGH_RISK";
        } else if (maxScore >= 20) {
            classification = "MEDIUM_RISK";
        }

        ClientRiskProfile profile = new ClientRiskProfile(iban, total, avgScore, maxScore, classification, recommendSuspension);
        return ResponseEntity.ok(profile);
    }

    /**
     * Cierra y resuelve una alerta de fraude abierta.
     * PUT /api/v1/antifraud/alerts/{id}/resolve?status=RESOLVED_FRAUDULENT
     */
    @PutMapping("/alerts/{id}/resolve")
    public ResponseEntity<?> resolveAlert(
            @PathVariable("id") Long id,
            @RequestParam("status") FraudAlert.AlertStatus newStatus) {

        return fraudAlertRepository.findById(id)
                .map(alert -> {
                    // Validamos que no se intente reabrir a estado pendiente
                    if (newStatus == FraudAlert.AlertStatus.PENDING_REVIEW) {
                        return ResponseEntity.badRequest().body("No está permitido volver a marcar una alerta como pendiente.");
                    }

                    alert.setStatus(newStatus);
                    fraudAlertRepository.save(alert);

                    System.out.println(">>> [AntiFraudRestController] Alerta ID " + id + " resuelta como: " + newStatus);
                    return ResponseEntity.ok(alert);
                })
                .orElse(ResponseEntity.notFound().build());
    }
}