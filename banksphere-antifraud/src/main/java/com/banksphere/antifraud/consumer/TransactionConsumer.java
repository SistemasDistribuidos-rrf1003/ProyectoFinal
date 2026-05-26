package com.banksphere.antifraud.consumer;

import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import java.math.BigDecimal;

/**
 * Consumidor de eventos que se suscribe de manera asíncrona a la cola 'transfer.queue' de RabbitMQ.
 * Recibe, deserializa e inicia la auditoría de riesgos del motor Antifraude.
 */
@Component
public class TransactionConsumer {

    // Simula una inyección del servicio encargado de procesar las reglas de fraude
    // (Implementaremos este servicio a continuación)
    // private final AntiFraudService antiFraudService;

    @Autowired
    public TransactionConsumer() {
        // En producción se inyectará el motor de reglas
        // this.antiFraudService = antiFraudService;
    }

    /**
     * Representación exacta del DTO plano (Payload) enviado por el Core.
     * Jackson mapea las propiedades JSON del mensaje a estas variables de manera nativa.
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
     * Escuchador asíncrono (Message Listener).
     * Se activa inmediatamente cuando RabbitMQ deposita una transacción en la cola 'transfer.queue'.
     */
    @RabbitListener(queues = "transfer.queue")
    public void consumeTransferEvent(TransferEvent event) {
        System.out.println("<<< [Event-Consumer] Evento de transferencia recibido con éxito.");
        System.out.println("    [ID Transacción]: " + event.id());
        System.out.println("    [Origen (IBAN)] : " + event.sourceIban() + " (" + event.sourceUserEmail() + ")");
        System.out.println("    [Destino (IBAN)]: " + event.destinationIban() + " (" + event.destinationUserEmail() + ")");
        System.out.println("    [Importe Neto]  : " + event.amount() + " | Comisión: " + event.fee());
        System.out.println("    [Fecha Emisión] : " + event.createdAt());

        // TODO: Invocar al motor de prevención para aplicar reglas AML y scoring
        // antiFraudService.analyzeTransaction(event);
    }
}