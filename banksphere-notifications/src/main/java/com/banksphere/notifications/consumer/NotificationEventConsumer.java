package com.banksphere.notifications.consumer;

import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

/**
 * Consumidor distribuido que actúa como puente reactivo:
 * Escucha eventos en RabbitMQ y los retransmite en tiempo real al navegador web vía WebSockets.
 */
@Component
public class NotificationEventConsumer {

    private final SimpMessagingTemplate messagingTemplate; // Plantilla de envío de WebSockets
    // private final MailSenderService mailSenderService;   // Inyectado para Fase 8 (Emails)

    @Autowired
    public NotificationEventConsumer(SimpMessagingTemplate messagingTemplate) {
        this.messagingTemplate = messagingTemplate;
        // this.mailSenderService = mailSenderService;
    }

    /**
     * DTO plano idéntico de transferencia. Mapea el payload serializado en JSON que viaja por RabbitMQ.
     */
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

    /**
     * Escuchador de RabbitMQ.
     * Consume transacciones de 'transfer.queue' de forma asíncrona.
     */
    @RabbitListener(queues = "transfer.queue")
    public void handleTransferEvent(TransferEvent event) {
        System.out.println("<<< [Notifications-Broker] Evento interceptado en RabbitMQ. ID: " + event.id());

        // 1. TRANSMISIÓN PUSH EN TIEMPO REAL VÍA WEBSOCKETS
        // Difundimos el evento al canal general '/topic/notifications'
        // Cualquier navegador conectado y suscrito a esta ruta reaccionará de inmediato
        try {
            messagingTemplate.convertAndSend("/topic/notifications", event);
            System.out.println(">>> [WebSocket-Push] Notificación enviada exitosamente a /topic/notifications");
        } catch (Exception e) {
            System.err.println(">>> [WebSocket-Error] Fallo al difundir mensaje push: " + e.getMessage());
        }

        // 2. [Fase 8: Emails] - Envío asíncrono de comprobante de seguridad por correo
        try {
            // mailSenderService.sendTransferEmail(event);
            System.out.println(">>> [Mail-Task] Disparado envío de email de confirmación para: " + event.sourceUserEmail());
        } catch (Exception e) {
            System.err.println(">>> [Mail-Error] Fallo al disparar el proceso de correo: " + e.getMessage());
        }
    }
}