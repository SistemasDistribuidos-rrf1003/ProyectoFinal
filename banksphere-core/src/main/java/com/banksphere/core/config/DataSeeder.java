package com.banksphere.core.config;

import com.banksphere.core.repository.UserRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class DataSeeder implements CommandLineRunner {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public DataSeeder(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    @Transactional
    public void run(String... args) {
        String correctHash = passwordEncoder.encode("password123");
        System.out.println(">>> [DATA-SEEDER] Hash generado para 'password123': " + correctHash);

        userRepository.findAll().forEach(user -> {
            user.setPassword(correctHash);
            userRepository.save(user);
            System.out.println(">>> [DATA-SEEDER] Contraseña actualizada para: " + user.getEmail());
        });

        System.out.println(">>> [DATA-SEEDER] ¡Todas las contraseñas han sido actualizadas correctamente!");
    }
}