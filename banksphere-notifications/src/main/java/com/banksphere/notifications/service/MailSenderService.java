package com.banksphere.notifications.service;

import com.banksphere.notifications.consumer.NotificationEventConsumer.TransferEvent;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import java.io.UnsupportedEncodingException;
import java.math.BigDecimal;

/**
 * Servicio encargado del envío de correos electrónicos enriquecidos (HTML).
 * Utiliza programáticamente el motor de Thymeleaf para renderizar plantillas responsivas en tiempo de ejecución.
 */
@Service
public class MailSenderService {

    private final JavaMailSender mailSender;
    private final TemplateEngine templateEngine; // Motor de renderizado dinámico de Thymeleaf

    @Autowired
    public MailSenderService(JavaMailSender mailSender, TemplateEngine templateEngine) {
        this.mailSender = mailSender;
        this.templateEngine = templateEngine;
    }

    /**
     * Envía un correo electrónico de bienvenida tras el registro del usuario.
     */
    public void sendWelcomeEmail(String recipientEmail, String clientName) {
        // 1. Cargamos el contexto de variables dinámicas para Thymeleaf
        Context thymeleafContext = new Context();
        thymeleafContext.setVariable("clientName", clientName);

        // 2. Renderizamos la plantilla HTML ubicada en templates/emails/welcome.html
        String htmlContent = templateEngine.process("emails/welcome", thymeleafContext);

        // 3. Despachamos el correo HTML
        sendHtmlEmail(recipientEmail, "¡Bienvenido a la revolución financiera de BankSphere!", htmlContent);
    }

    /**
     * Envía un comprobante de transferencia exitosa en formato digital.
     */
    public void sendTransferEmail(TransferEvent event) {
        // 1. Inyectamos las variables del evento AMQP en el contexto de Thymeleaf
        Context thymeleafContext = new Context();
        thymeleafContext.setVariable("id", event.id());
        thymeleafContext.setVariable("sourceIban", event.sourceIban());
        thymeleafContext.setVariable("destinationIban", event.destinationIban());
        thymeleafContext.setVariable("destinationEmail", event.destinationUserEmail());
        thymeleafContext.setVariable("amount", event.amount());
        thymeleafContext.setVariable("fee", event.fee());
        thymeleafContext.setVariable("concept", event.concept());
        thymeleafContext.setVariable("type", event.transferType());
        thymeleafContext.setVariable("date", event.createdAt());

        // 2. Renderizamos la plantilla HTML de comprobante
        String htmlContent = templateEngine.process("emails/transfer-alert", thymeleafContext);

        // 3. Enviamos el comprobante digital al emisor de la transferencia
        sendHtmlEmail(event.sourceUserEmail(), "Comprobante Digital de Transferencia - BankSphere", htmlContent);
    }

    /**
     * Envía una alerta roja de ciberseguridad / AML si el motor de riesgos congela una cuenta.
     */
    public void sendSecurityAlertEmail(String recipientEmail, String clientName, String reason, String blockedIban) {
        Context thymeleafContext = new Context();
        thymeleafContext.setVariable("clientName", clientName);
        thymeleafContext.setVariable("reason", reason);
        thymeleafContext.setVariable("iban", blockedIban);

        String htmlContent = templateEngine.process("emails/security-alert", thymeleafContext);

        sendHtmlEmail(recipientEmail, "⚠️ ALERTA DE SEGURIDAD CRÍTICA - Cuenta Congelada", htmlContent);
    }

    /**
     * Método helper interno y reutilizable que encapsula el protocolo MIME para despachar correos en HTML.
     */
    private void sendHtmlEmail(String to, String subject, String htmlBody) {
        try {
            // Creamos un mensaje MIME (soporta codificación rica y archivos adjuntos)
            MimeMessage mimeMessage = mailSender.createMimeMessage();

            // Asistente helper para simplificar la declaración de cabeceras
            MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, true, "UTF-8");

            // Configuración del Remitente con un Alias amigable
            helper.setFrom("no-reply@banksphere.com", "BankSphere Seguridad");
            helper.setTo(to);
            helper.setSubject(subject);

            // Inyectamos el HTML renderizado (el segundo parámetro 'true' habilita la interpretación de etiquetas HTML)
            helper.setText(htmlBody, true);

            // Despachamos al SMTP de Mailpit
            mailSender.send(mimeMessage);
            System.out.println(">>> [Mail-Service] Correo HTML enviado con éxito a: " + to + " (Asunto: " + subject + ")");

        } catch (MessagingException | UnsupportedEncodingException e) {
            System.err.println(">>> [Mail-Service-Error] Fallo al despachar correo electrónico MIME: " + e.getMessage());
            throw new RuntimeException("Fallo en el servicio de correo: " + e.getMessage(), e);
        }
    }
}