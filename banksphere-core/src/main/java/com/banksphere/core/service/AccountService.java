package com.banksphere.core.service;

import com.banksphere.core.entity.Account;
import com.banksphere.core.repository.AccountRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Servicio de negocio transaccional para la gestión de cuentas bancarias en BankSphere.
 */
@Service
public class AccountService {

    private final AccountRepository accountRepository;

    @Autowired
    public AccountService(AccountRepository accountRepository) {
        this.accountRepository = accountRepository;
    }

    /**
     * Recupera la lista completa de cuentas registradas en el banco.
     * Optimizado como transacción de solo lectura.
     */
    @Transactional(readOnly = true)
    public List<Account> getAllAccounts() {
        return accountRepository.findAll();
    }

    /**
     * Recupera las cuentas asociadas a un cliente mediante su email de sesión.
     */
    @Transactional(readOnly = true)
    public List<Account> getAccountsByUserEmail(String email) {
        if (email == null || email.trim().isEmpty()) {
            throw new IllegalArgumentException("El email del usuario no puede estar vacío.");
        }
        return accountRepository.findByUserEmail(email);
    }

    /**
     * Recupera una cuenta bancaria por su identificador numérico único.
     */
    @Transactional(readOnly = true)
    public Account getAccountById(Long id) {
        return accountRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Cuenta bancaria no encontrada con ID: " + id));
    }

    /**
     * Recupera una cuenta bancaria buscando por su IBAN.
     */
    @Transactional(readOnly = true)
    public Account getAccountByIban(String iban) {
        return accountRepository.findByIban(iban)
                .orElseThrow(() -> new RuntimeException("Cuenta bancaria no encontrada con IBAN: " + iban));
    }

    /**
     * Registra y abre una nueva cuenta bancaria en el sistema.
     * Valida que el titular no sea nulo y que no haya duplicidad de IBAN.
     */
    @Transactional
    public Account createAccount(Account account) {
        if (account.getUser() == null) {
            throw new IllegalArgumentException("No se puede crear una cuenta bancaria sin un titular asignado.");
        }

        // Si el IBAN fue asignado manualmente, validamos que no exista duplicidad
        if (account.getIban() != null && accountRepository.existsByIban(account.getIban())) {
            throw new IllegalArgumentException("Ya existe una cuenta registrada con el IBAN: " + account.getIban());
        }

        return accountRepository.save(account);
    }

    /**
     * Actualiza los datos de una cuenta existente (ej. saldos tras transferencias o estados).
     */
    @Transactional
    public Account updateAccount(Account account) {
        getAccountById(account.getId());
        return accountRepository.save(account);
    }

    /**
     * Elimina físicamente una cuenta bancaria de la base de datos.
     */
    @Transactional
    public void deleteAccount(Long id) {
        Account account = getAccountById(id);
        accountRepository.delete(account);
    }
}
