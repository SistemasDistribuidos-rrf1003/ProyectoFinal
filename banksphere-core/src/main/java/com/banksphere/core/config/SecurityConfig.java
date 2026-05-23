package com.banksphere.core.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;

import java.io.IOException;
import java.util.Collection;

/**
 * Configuración de seguridad global para la aplicación BankSphere.
 * Implementa cifrado BCrypt, control de accesos basados en roles,
 * protección CSRF inteligente y direccionamiento dinámico tras autenticación exitosa.
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity // Permite seguridad declarativa con @PreAuthorize / @Secured en controladores
public class SecurityConfig {

    /**
     * Definición del Bean de cifrado de contraseñas.
     * Utiliza el algoritmo BCrypt con fuerza de ronda estándar (10).
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    /**
     * Configuración del pipeline de filtros de seguridad (Security Filter Chain).
     */
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                // 1. Protección contra ataques CSRF (Cross-Site Request Forgery)
                .csrf(csrf -> csrf
                        // Excluimos endpoints API (REST Open Banking) ya que se protegerán mediante tokens/API Keys
                        .ignoringRequestMatchers("/api/v1/openbanking/**", "/h2-console/**")
                )

                // 2. Control y autorización de peticiones HTTP
                .authorizeHttpRequests(auth -> auth
                        // Rutas públicas y recursos estáticos sin necesidad de sesión
                        .requestMatchers("/", "/landing", "/login", "/register", "/css/**", "/js/**", "/assets/**", "/webjars/**").permitAll()

                        // Habilitación de consola H2 si se usa para pruebas
                        .requestMatchers("/h2-console/**").permitAll()

                        // Rutas exclusivas para el rol ADMINISTRADOR (Gestión de usuarios y sistema)
                        .requestMatchers("/users/**", "/admin/**").hasRole("ADMIN")

                        // Rutas exclusivas para el rol ANALISTA FINANCIERO (Prevención de fraude, KYC y AML)
                        .requestMatchers("/analyst/**", "/antifraud/**").hasRole("ANALYST")

                        // Rutas exclusivas para el rol CLIENTE / USUARIO NORMAL (Operaciones bancarias y balance)
                        .requestMatchers("/dashboard/**", "/accounts/**", "/transfers/**").hasRole("USER")

                        // Cualquier otra petición debe estar autenticada
                        .anyRequest().authenticated()
                )

                // 3. Configuración de página de Login personalizada
                .formLogin(form -> form
                        .loginPage("/login")
                        .loginProcessingUrl("/login")
                        .successHandler(customAuthenticationSuccessHandler())
                        .failureUrl("/login?error=true")
                        .permitAll()
                )

                // 4. Configuración del proceso de Logout (Cierre de sesión)
                .logout(logout -> logout
                        .logoutUrl("/logout")
                        .logoutSuccessUrl("/login?logout=true")
                        .invalidateHttpSession(true)
                        .clearAuthentication(true)
                        .deleteCookies("JSESSIONID")
                        .permitAll()
                )

                // 5. Configuración para evitar problemas de renderizado en IFRAMEs (Consola H2)
                .headers(headers -> headers
                        .frameOptions(frame -> frame.sameOrigin())
                );

        return http.build();
    }

    /**
     * Manejador de éxito personalizado (Success Handler) que redirige al usuario a su
     * módulo correspondiente dependiendo del rol con el que inicie sesión.
     *
     * - Si es ADMIN -> Se le redirige al listado CRUD de gestión de usuarios (`/users`)
     * - Si es ANALYST -> Se le redirige al panel de control de alertas y fraudes (`/analyst/dashboard`)
     * - Si es USER (Cliente) -> Se le redirige a su panel financiero personal (`/dashboard`)
     */
    @Bean
    public AuthenticationSuccessHandler customAuthenticationSuccessHandler() {
        return (request, response, authentication) -> {
            Collection<? extends GrantedAuthority> authorities = authentication.getAuthorities();

            String redirectUrl = "/dashboard";

            for (GrantedAuthority authority : authorities) {
                String role = authority.getAuthority();
                if (role.equals("ROLE_ADMIN")) {
                    redirectUrl = "/users";
                    break;
                } else if (role.equals("ROLE_ANALYST")) {
                    redirectUrl = "/analyst/dashboard";
                    break;
                } else if (role.equals("ROLE_USER")) {
                    redirectUrl = "/dashboard";
                    break;
                }
            }

            response.sendRedirect(redirectUrl);
        };
    }
}
