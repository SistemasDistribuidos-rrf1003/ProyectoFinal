package com.banksphere.notifications.controller;

import com.banksphere.notifications.model.ChatMessage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;

import java.util.concurrent.CompletableFuture;

/**
 * Controlador Spring STOMP para gestionar el canal de comunicación del chat de soporte técnico.
 */
@Controller
public class WebSocketChatController {

    @Autowired
    private SimpMessagingTemplate messagingTemplate;

    /**
     * Recibe los mensajes del chat y los difunde a todos los usuarios/analistas enrutados.
     */
    @MessageMapping("/chat.sendMessage")
    @SendTo("/topic/support")
    public ChatMessage sendMessage(@Payload ChatMessage chatMessage) {

        // Si el mensaje es de un cliente normal, el Bot responde automáticamente
        if (chatMessage.getType() == ChatMessage.MessageType.CHAT && !chatMessage.getSender().equals("Agente_BankSphere")) {
            CompletableFuture.runAsync(() -> {
                try {
                    // Simulamos que el agente tarda 1.5 segundos en escribir
                    Thread.sleep(1500);
                } catch (InterruptedException e) {}

                ChatMessage reply = new ChatMessage();
                reply.setType(ChatMessage.MessageType.CHAT);
                reply.setSender("Agente_BankSphere");
                reply.setContent("¡Hola, " + chatMessage.getSender() + "! Soy el asistente virtual de BankSphere. Actualmente todos nuestros agentes humanos están ocupados. Por favor, detalla tu incidencia aquí o envíanos un email a soporte@banksphere.com y te atenderemos en breve.");

                // Enviamos la respuesta automática de vuelta al canal
                messagingTemplate.convertAndSend("/topic/support", reply);
            });
        }

        return chatMessage;
    }

    /**
     * Gestiona la conexión inicial de un usuario a la sala de soporte.
     */
    @MessageMapping("/chat.addUser")
    @SendTo("/topic/support")
    public ChatMessage addUser(@Payload ChatMessage chatMessage, SimpMessageHeaderAccessor headerAccessor) {
        if (headerAccessor.getSessionAttributes() != null) {
            headerAccessor.getSessionAttributes().put("username", chatMessage.getSender());
        }
        return chatMessage;
    }
}