package com.banksphere.notifications.model;

import lombok.*;

/**
 * DTO que modela la estructura de un mensaje enviado en el chat de soporte en vivo.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ChatMessage {
    private String sender;
    private String content;
    private MessageType type;

    public enum MessageType {
        CHAT,   // Mensaje de texto normal
        JOIN,   // Usuario se conecta al chat
        LEAVE   // Usuario abandona el soporte
    }
}