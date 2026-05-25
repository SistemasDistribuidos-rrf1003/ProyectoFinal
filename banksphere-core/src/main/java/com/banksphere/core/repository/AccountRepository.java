package com.banksphere.core.repository;

import com.banksphere.core.entity.Account;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repositorio Spring Data JPA para la gestión de persistencia de Cuentas Bancarias.
 */
@Repository
public interface AccountRepository extends JpaRepository<Account, Long> {

    /**
     * Recupera todas las cuentas que pertenecen a un cliente buscando por su correo electrónico.
     */
    List<Account> findByUserEmail(String email);

    /**
     * Recupera una cuenta bancaria mediante su código IBAN único.
     */
    Optional<Account> findByIban(String iban);

    /**
     * Verifica si ya existe una cuenta registrada con un IBAN específico.
     */
    boolean existsByIban(String iban);
}