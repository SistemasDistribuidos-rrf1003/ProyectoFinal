package com.banksphere.antifraud.config;

import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Configuración de RabbitMQ para el consumidor.
 * Declara el bean MessageConverter necesario para deserializar los mensajes JSON entrantes.
 */
@Configuration
public class RabbitMQConsumerConfig {

    /**
     * Define el deserializador de Jackson para convertir automáticamente los mensajes JSON
     * de las colas de RabbitMQ en objetos Java (records/DTOs).
     */
    @Bean
    public MessageConverter consumerJackson2MessageConverter() {
        return new Jackson2JsonMessageConverter();
    }
}