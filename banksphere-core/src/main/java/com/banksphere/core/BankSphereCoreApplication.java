package com.banksphere.core;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.transaction.annotation.EnableTransactionManagement;

/**
 * Punto de entrada principal para el microservicio Core de la plataforma BankSphere.
 *
 * Se configuran explícitamente los paquetes de escaneo de componentes para permitir que
 * Spring Boot detecte e inyecte los DTOs y el GlobalExceptionHandler ubicados en el
 * submódulo compartido 'banksphere-common'.
 */
@SpringBootApplication(scanBasePackages = {
        "com.banksphere.core",          // Escanea controladores, servicios y configuraciones del Core
        "com.banksphere.common"         // Escanea excepciones y el GlobalExceptionHandler del módulo común
})
@EntityScan(basePackages = "com.banksphere.core.entity") // Asegura el escaneo de entidades JPA de la base de datos
@EnableJpaRepositories(basePackages = "com.banksphere.core.repository") // Habilita la inyección de repositorios JPA
@EnableTransactionManagement // Habilita el control transaccional robusto (@Transactional) para transferencias
public class BankSphereCoreApplication {

    public static void main(String[] args) {
        SpringApplication.run(BankSphereCoreApplication.class, args);
        System.out.println("====================================================");
        System.out.println("   BankSphere Core Service - Levantado Exitosamente ");
        System.out.println("   Acceso Web: http://localhost:8080                ");
        System.out.println("   Documentación API Swagger: /swagger-ui.html      ");
        System.out.println("====================================================");
    }
}