package com.banksphere.notifications.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

/**
 * Configuración de WebSockets en el microservicio de notificaciones.
 * Habilita y parametriza el Broker de Mensajería STOMP sobre WebSockets.
 */
@Configuration
@EnableWebSocketMessageBroker // Habilita el procesamiento de mensajería asíncrona sobre WebSocket
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    /**
     * Registra los puntos de conexión (Endpoints) que el cliente web (frontend)
     * utilizará para establecer la conexión inicial de WebSockets (Handshake).
     */
    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        registry.addEndpoint("/ws")                  // Punto de entrada WebSocket establecido en '/ws'
                .setAllowedOriginPatterns("*")        // CRÍTICO: Habilita CORS para permitir conexiones cruzadas desde el Core (Puerto 8080)
                .withSockJS();                       // Habilita SockJS como mecanismo de fallback en navegadores antiguos sin soporte nativo de WS
    }

    /**
     * Configura el Broker de Mensajes interno para enrutar los flujos de comunicación.
     */
    @Override
    public void configureMessageBroker(MessageBrokerRegistry registry) {

        // Habilita un broker de mensajes simple basado en memoria.
        // Los prefijos definen los destinos de los mensajes salientes (del servidor al cliente):
        // - '/topic' -> Para suscripciones generales de difusión (Broadcast, ej: Chat o Notificaciones globales)
        // - '/queue' -> Para notificaciones privadas punto a punto a usuarios específicos
        registry.enableSimpleBroker("/topic", "/queue");

        // Define el prefijo que los clientes utilizarán para enviar mensajes hacia el servidor (del cliente al servidor):
        // Ejemplo: Si un cliente envía al chat de soporte, enviará al endpoint '/app/chat.sendMessage'
        registry.setApplicationDestinationPrefixes("/app");
    }
}