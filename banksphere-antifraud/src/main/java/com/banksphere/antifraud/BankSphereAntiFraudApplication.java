package com.banksphere.antifraud;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Punto de entrada principal para el microservicio de prevención de fraude BankSphere AntiFraud.
 * Arranca de forma autónoma e independiente del módulo Core.
 */
@SpringBootApplication
public class BankSphereAntiFraudApplication {

    public static void main(String[] args) {
        SpringApplication.run(BankSphereAntiFraudApplication.class, args);
    }
}