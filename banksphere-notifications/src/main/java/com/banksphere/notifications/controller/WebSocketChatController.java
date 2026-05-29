package com.banksphere.notifications.controller;

import com.banksphere.notifications.model.ChatMessage;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.stereotype.Controller;

/**
 * Controlador Spring STOMP para gestionar el canal de comunicación del chat de soporte técnico.
 */
@Controller
public class WebSocketChatController {

    /**
     * Recibe los mensajes del chat y los difunde a todos los usuarios/analistas enrutados.
     * Endpoint de entrada: /app/chat.sendMessage
     * Endpoint de salida: /topic/support
     */
    @MessageMapping("/chat.sendMessage")
    @SendTo("/topic/support")
    public ChatMessage sendMessage(@Payload ChatMessage chatMessage) {
        return chatMessage; // Se difunde de forma automática en formato JSON
    }

    /**
     * Gestiona la conexión inicial de un usuario a la sala de soporte.
     * Enlaza el nombre del usuario a la sesión WebSocket de Spring.
     * Endpoint de entrada: /app/chat.addUser
     * Endpoint de salida: /topic/support
     */
    @MessageMapping("/chat.addUser")
    @SendTo("/topic/support")
    public ChatMessage addUser(@Payload ChatMessage chatMessage, SimpMessageHeaderAccessor headerAccessor) {
        // Guardamos el nombre de usuario en los atributos de la sesión WebSocket
        if (headerAccessor.getSessionAttributes() != null) {
            headerAccessor.getSessionAttributes().put("username", chatMessage.getSender());
        }
        return chatMessage;
    }
}