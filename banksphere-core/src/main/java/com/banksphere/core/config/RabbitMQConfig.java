package com.banksphere.core.config;

import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Clase de configuración para la integración distribuida con RabbitMQ.
 * Define la estructura del broker (Exchanges, Queues y Bindings) y configura
 * la serialización segura de objetos en formato JSON para la red.
 */
@Configuration
public class RabbitMQConfig {

    // Nombres lógicos de la infraestructura de mensajería
    public static final String EXCHANGE_NAME = "banksphere.exchange";
    public static final String TRANSFER_QUEUE = "transfer.queue";
    public static final String TRANSFER_ROUTING_KEY = "banksphere.transfer.completed";

    /**
     * Define el Exchange principal del banco de tipo TOPIC.
     * Los intercambiadores de tipo Topic permiten un enrutamiento sumamente flexible
     * mediante claves de enrutamiento (Routing Keys) con comodines (*, #).
     */
    @Bean
    public TopicExchange banksphereExchange() {
        return new TopicExchange(EXCHANGE_NAME, true, false);
    }

    /**
     * Define la cola de transferencias completadas.
     * Se configura como DURABLE (persiste en el disco duro de RabbitMQ) para asegurar
     * que ningún mensaje se pierda en vuelo en caso de que el servidor de mensajería se reinicie.
     */
    @Bean
    public Queue transferQueue() {
        return QueueBuilder.durable(TRANSFER_QUEUE).build();
    }

    /**
     * Enlaza (Binding) la cola de transferencias al Topic Exchange principal
     * a través de una clave de enrutamiento fija (Routing Key).
     */
    @Bean
    public Binding transferBinding(Queue transferQueue, TopicExchange banksphereExchange) {
        return BindingBuilder
                .bind(transferQueue)
                .to(banksphereExchange())
                .with(TRANSFER_ROUTING_KEY);
    }

    /**
     * Configura el convertidor de mensajes de Spring AMQP a formato JSON usando Jackson.
     * Esto asegura que cualquier DTO de transferencia enviado sea serializado automáticamente a JSON.
     */
    @Bean
    public MessageConverter producerJackson2MessageConverter() {
        return new Jackson2JsonMessageConverter();
    }

    /**
     * Sobrescribe y configura la plantilla RabbitTemplate inyectando el convertidor JSON.
     * Es la herramienta de Spring que utilizará el 'TransferService' para publicar los mensajes de forma ágil.
     */
    @Bean
    public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory) {
        RabbitTemplate rabbitTemplate = new RabbitTemplate(connectionFactory);
        rabbitTemplate.setMessageConverter(producerJackson2MessageConverter());
        return rabbitTemplate;
    }
}