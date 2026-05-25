package com.banksphere.core.repository;

import com.banksphere.core.entity.Transfer;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Repositorio Spring Data JPA para la gestión del historial de transferencias.
 */
@Repository
public interface TransferRepository extends JpaRepository<Transfer, Long> {

    /**
     * Recupera el historial completo de transferencias (tanto enviadas como recibidas) de una cuenta específica.
     */
    @Query("SELECT t FROM Transfer t WHERE t.sourceAccount.id = :accountId OR t.destinationAccount.id = :accountId ORDER BY t.createdAt DESC")
    List<Transfer> findAllMovementsByAccountId(@Param("accountId") Long accountId);

    /**
     * Recupera el historial paginado de transferencias para un usuario específico.
     */
    @Query("SELECT t FROM Transfer t WHERE t.sourceAccount.user.id = :userId OR t.destinationAccount.user.id = :userId ORDER BY t.createdAt DESC")
    Page<Transfer> findMovementsByUserId(@Param("userId") Long userId, Pageable pageable);
}