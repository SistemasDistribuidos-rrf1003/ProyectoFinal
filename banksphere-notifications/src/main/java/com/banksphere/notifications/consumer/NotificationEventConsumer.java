package com.banksphere.notifications.consumer;

import com.banksphere.notifications.service.MailSenderService;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

/**
 * Consumidor Reactivo y Orquestador de Alertas.
 * Escucha eventos distribuidos de RabbitMQ y despacha de forma asíncrona notificaciones push e emails HTML.
 */
@Component
public class NotificationEventConsumer {

    private final SimpMessagingTemplate messagingTemplate;
    private final MailSenderService mailSenderService;

    @Autowired
    public NotificationEventConsumer(SimpMessagingTemplate messagingTemplate, MailSenderService mailSenderService) {
        this.messagingTemplate = messagingTemplate;
        this.mailSenderService = mailSenderService;
    }

    // ESTRUCTURA DE EVENTOS (PAYLOADS JSON)

    public record TransferEvent(
            Long id,
            String sourceIban,
            String sourceUserEmail,
            String destinationIban,
            String destinationUserEmail,
            BigDecimal amount,
            BigDecimal fee,
            String concept,
            String transferType,
            String createdAt
    ) {}

    public record UserRegistrationEvent(
            String email,
            String firstName,
            String lastName,
            String nationalId
    ) {}

    public record FraudAlertEvent(
            Long transferId,
            String sourceIban,
            String userEmail,
            String userName,
            Integer riskScore,
            String riskLevel,
            String triggeredRules,
            String reason
    ) {}

    // =========================================================================
    // 1. DISPARADOR DE COMPROBANTES DE TRANSFERENCIA (transfer.queue)
    // =========================================================================
    @RabbitListener(queues = "transfer.queue")
    public void handleTransferEvent(TransferEvent event) {
        System.out.println("<<< [Event-Trigger] Procesando comprobante de transferencia ID: " + event.id());

        // 1. Alerta Push interactiva al navegador
        messagingTemplate.convertAndSend("/topic/notifications", event);

        // 2. Envío de comprobante en HTML por Email
        mailSenderService.sendTransferEmail(event);
    }

    // =========================================================================
    // 2. DISPARADOR DE CORREOS DE BIENVENIDA (user.registration.queue)
    // =========================================================================
    @RabbitListener(queues = "user.registration.queue")
    public void handleUserRegistrationEvent(UserRegistrationEvent event) {
        System.out.println("<<< [Event-Trigger] Nuevo cliente registrado. Disparando bienvenida para: " + event.email());

        // 1. Envío asíncrono del correo de bienvenida e inicio de KYC
        mailSenderService.sendWelcomeEmail(event.email(), event.firstName() + " " + event.lastName());
    }

    // =========================================================================
    // 3. DISPARADOR DE CORREOS DE ALERTA CRÍTICA DE FRAUDE (fraud.alert.queue)
    // =========================================================================
    @RabbitListener(queues = "fraud.alert.queue")
    public void handleFraudAlertEvent(FraudAlertEvent event) {
        System.out.println("<<< [Event-Trigger] ¡ALERTA CRÍTICA DE RIESGO! Interceptado fraude en transfer ID: " + event.transferId());

        // 1. Difundimos una notificación push de ciberseguridad especial al canal de seguridad
        messagingTemplate.convertAndSend("/topic/security", event);

        // 2. Envío del correo urgente de seguridad de cuenta congelada / suspendida
        mailSenderService.sendSecurityAlertEmail(
                event.userEmail(),
                event.userName(),
                event.reason(),
                event.sourceIban()
        );
    }
}