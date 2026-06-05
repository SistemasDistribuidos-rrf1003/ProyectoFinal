package com.banksphere.core.service;

import com.banksphere.core.entity.User;
import com.banksphere.core.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;

/**
 * Servicio de negocio para la gestión de usuarios.
 * Imprime las reglas de negocio de creación, búsqueda y actualización.
 * Implementa {@link UserDetailsService} para integrarse directamente con Spring Security.
 */
@Service
public class UserService implements UserDetailsService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Autowired
    public UserService(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    /**
     * Carga un usuario de la base de datos por su email para la autenticación de Spring Security.
     * Mapea los roles internos de BankSphere al formato estándar 'ROLE_' requerido por Spring Security.
     */
    @Override
    @Transactional(readOnly = true)
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        System.out.println(">>> [DEBUG-LOGIN] Intentando cargar email: " + email);

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException("Usuario no encontrado con el email: " + email));

        System.out.println(">>> [DEBUG-LOGIN] Datos recuperados: Email=" + user.getEmail()
                + " | Estado=" + user.getStatus()
                + " | Rol=" + user.getRole()
                + " | Hash=" + user.getPassword());

        // Mapeo del rol de la entidad (ADMIN, USER, ANALYST) al formato de Spring Security (ROLE_ADMIN, etc.)
        SimpleGrantedAuthority authority = new SimpleGrantedAuthority("ROLE_" + user.getRole().name());

        // Control de estado de la cuenta del usuario para la sesión de seguridad
        boolean enabled = (user.getStatus() == User.UserStatus.ACTIVE || user.getStatus() == User.UserStatus.PENDING_KYC);
        boolean accountNonLocked = (user.getStatus() != User.UserStatus.SUSPENDED);
        return new org.springframework.security.core.userdetails.User(
                user.getEmail(),
                user.getPassword(),
                enabled,
                true, // accountNonExpired
                true, // credentialsNonExpired
                accountNonLocked,
                Collections.singletonList(authority)
        );
    }

    /**
     * Recupera un usuario por su identificador único.
     */
    @Transactional(readOnly = true)
    public User getUserById(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado con ID: " + id));
    }

    /**
     * Recupera todos los usuarios con soporte de paginación para el panel de administración.
     */
    @Transactional(readOnly = true)
    public Page<User> getAllUsers(Pageable pageable) {
        return userRepository.findAll(pageable);
    }

    /**
     * Búsqueda avanzada de usuarios por texto libre.
     */
    @Transactional(readOnly = true)
    public Page<User> searchUsers(String searchTerm, Pageable pageable) {
        if (searchTerm == null || searchTerm.trim().isEmpty()) {
            return userRepository.findAll(pageable);
        }
        return userRepository.searchUsers(searchTerm.trim(), pageable);
    }

    /**
     * Registra / Crea un nuevo usuario encriptando su contraseña con BCrypt.
     */
    @Transactional
    public User createUser(User user) {
        if (userRepository.existsByEmail(user.getEmail())) {
            throw new IllegalArgumentException("Ya existe un usuario registrado con el email: " + user.getEmail());
        }
        if (userRepository.existsByNationalId(user.getNationalId())) {
            throw new IllegalArgumentException("Ya existe un usuario con el documento de identidad: " + user.getNationalId());
        }

        // Cifrado obligatorio de contraseña con BCrypt
        user.setPassword(passwordEncoder.encode(user.getPassword()));
        
        return userRepository.save(user);
    }

    /**
     * Actualiza los datos de un usuario existente.
     */
    @Transactional
    public User updateUser(Long id, User userDetails) {
        User user = getUserById(id);

        user.setFirstName(userDetails.getFirstName());
        user.setLastName(userDetails.getLastName());
        user.setPhone(userDetails.getPhone());
        user.setRole(userDetails.getRole());
        user.setStatus(userDetails.getStatus());

        // Solo se actualiza la contraseña si se proporciona una nueva
        if (userDetails.getPassword() != null && !userDetails.getPassword().isEmpty() 
            && !userDetails.getPassword().equals(user.getPassword())) {
            user.setPassword(passwordEncoder.encode(userDetails.getPassword()));
        }

        return userRepository.save(user);
    }

    /**
     * Elimina lógicamente o físicamente un usuario del sistema.
     */
    @Transactional
    public void deleteUser(Long id) {
        User user = getUserById(id);
        userRepository.delete(user);
    }

    /**
     * Recupera un usuario de la base de datos buscando por su dirección de correo electrónico.
     */
    @Transactional(readOnly = true)
    public User getUserByEmail(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException("Usuario no encontrado con email: " + email));
    }
}
