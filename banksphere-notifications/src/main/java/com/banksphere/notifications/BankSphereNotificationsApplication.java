package com.banksphere.notifications;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Punto de entrada principal para el microservicio de notificaciones BankSphere Notifications.
 * Gestiona el envío de emails SMTP y difunde alertas en tiempo real vía WebSockets.
 */
@SpringBootApplication
public class BankSphereNotificationsApplication {

    public static void main(String[] args) {
        SpringApplication.run(BankSphereNotificationsApplication.class, args);
    }
}