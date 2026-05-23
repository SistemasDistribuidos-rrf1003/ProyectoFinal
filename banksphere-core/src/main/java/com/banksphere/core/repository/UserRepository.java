package com.banksphere.core.repository;

import com.banksphere.core.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Repositorio Spring Data JPA para la entidad User.
 * Proporciona métodos de acceso a la base de datos para la gestión y autenticación de usuarios.
 */
@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    /**
     * Busca un usuario por su dirección de correo electrónico (utilizado en la autenticación).
     */
    Optional<User> findByEmail(String email);

    /**
     * Busca un usuario por su documento nacional de identidad (DNI / NIE / Pasaporte).
     */
    Optional<User> findByNationalId(String nationalId);

    /**
     * Comprueba si existe un usuario registrado con un email determinado.
     */
    boolean existsByEmail(String email);

    /**
     * Comprueba si existe un usuario registrado con un documento de identidad determinado.
     */
    boolean existsByNationalId(String nationalId);

    /**
     * Búsqueda avanzada con filtros y paginación para el panel de administración.
     * Permite buscar por nombre, apellidos, email o documento de identidad de forma insensible a mayúsculas.
     */
    @Query("SELECT u FROM User u WHERE " +
           "LOWER(u.firstName) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR " +
           "LOWER(u.lastName) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR " +
           "LOWER(u.email) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR " +
           "LOWER(u.nationalId) LIKE LOWER(CONCAT('%', :searchTerm, '%'))")
    Page<User> searchUsers(@Param("searchTerm") String searchTerm, Pageable pageable);
}
