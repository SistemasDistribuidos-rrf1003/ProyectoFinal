package com.banksphere.core.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;

import java.util.Collection;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

    /**
     * Definición del Bean de cifrado de contraseñas (BCrypt).
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    /**
     * =========================================================================
     * CADENA DE FILTROS 1: API REST DE OPEN BANKING (STATELESS + BASIC AUTH)
     * =========================================================================
     * Se activa únicamente para peticiones que comiencen con el prefijo /api/v1/openbanking/
     */
    @Bean
    @Order(1) // Máxima prioridad de evaluación
    public SecurityFilterChain apiSecurityFilterChain(HttpSecurity http) throws Exception {
        http
                .securityMatcher("/api/v1/openbanking/**") // Aplica solo a la API

                // 1. Deshabilitamos CSRF ya que es una API Stateless protegida por credenciales en cada request
                .csrf(csrf -> csrf.disable())

                // 2. Forzamos políticas de sesión sin estado (Stateless) para que no guarde cookies de sesión
                .sessionManagement(session -> session
                        .sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                )

                // 3. Reglas de Autorización de la API
                .authorizeHttpRequests(auth -> auth
                        // Todos los endpoints de Open Banking requieren autenticación básica previa
                        .anyRequest().authenticated()
                )

                // 4. Habilitamos HTTP Basic Authentication para aplicaciones clientes de terceros
                .httpBasic(Customizer.withDefaults());

        return http.build();
    }

    /**
     * =========================================================================
     * CADENA DE FILTROS 2: VISTAS THYMELEAF (STATEFUL + COOKIES + FORM LOGIN)
     * =========================================================================
     * Actúa como fallback para gestionar la seguridad visual y de sesión del usuario
     */
    @Bean
    @Order(2)
    public SecurityFilterChain webSecurityFilterChain(HttpSecurity http) throws Exception {
        http
                // 1. Protección CSRF obligatoria para vistas web
                .csrf(csrf -> csrf
                        .ignoringRequestMatchers("/h2-console/**")
                )

                // 2. Reglas de Autorización para vistas y recursos
                .authorizeHttpRequests(auth -> auth
                        // Recursos públicos
                        .requestMatchers("/", "/landing", "/login", "/register", "/css/**", "/js/**", "/assets/**", "/webjars/**").permitAll()
                        .requestMatchers("/h2-console/**").permitAll()

                        // Control de Accesos por Roles en la interfaz gráfica
                        .requestMatchers("/users/**", "/admin/**").hasRole("ADMIN")
                        .requestMatchers("/analyst/**", "/antifraud/**").hasRole("ANALYST")
                        .requestMatchers("/dashboard/**", "/accounts/**", "/transfers/**").hasRole("USER")

                        .anyRequest().authenticated()
                )

                // 3. Login por formulario personalizado
                .formLogin(form -> form
                        .loginPage("/login")
                        .loginProcessingUrl("/login")
                        .successHandler(customAuthenticationSuccessHandler())
                        .failureUrl("/login?error=true")
                        .permitAll()
                )

                // 4. Logout seguro
                .logout(logout -> logout
                        .logoutUrl("/logout")
                        .logoutSuccessUrl("/login?logout=true")
                        .invalidateHttpSession(true)
                        .clearAuthentication(true)
                        .deleteCookies("JSESSIONID")
                        .permitAll()
                )

                // 5. Configuración específica para habilitar la Consola H2 local en iFrames
                .headers(headers -> headers
                        .frameOptions(frame -> frame.sameOrigin())
                );

        return http.build();
    }

    /**
     * Manejador de éxito que redirecciona al usuario según su Rol al iniciar sesión en la Web.
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