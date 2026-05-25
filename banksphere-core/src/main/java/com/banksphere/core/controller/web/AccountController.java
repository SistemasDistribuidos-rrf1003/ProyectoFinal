package com.banksphere.core.controller.web;

import com.banksphere.core.entity.Account;
import com.banksphere.core.entity.User;
import com.banksphere.core.service.AccountService;
import com.banksphere.core.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.math.BigDecimal;
import java.util.List;

/**
 * Controlador Web MVC para la gestión de Cuentas Bancarias.
 * Implementa las reglas de visibilidad por Rol (USER vs ADMIN) y administración de saldos.
 */
@Controller
@RequestMapping("/accounts")
public class AccountController {

    private final AccountService accountService;
    private final UserService userService;

    @Autowired
    public AccountController(AccountService accountService, UserService userService) {
        this.accountService = accountService;
        this.userService = userService;
    }

    /**
     * Muestra el listado de cuentas.
     * Regla de Negocio:
     * - Un USER solo ve sus propias cuentas asociadas.
     * - Un ADMIN puede ver la lista consolidada de todas las cuentas del banco.
     */
    @GetMapping
    public String listAccounts(Model model, Authentication authentication) {
        String loggedEmail = authentication.getName();

        // Comprueba si el usuario autenticado tiene rol de Administrador
        boolean isAdmin = authentication.getAuthorities().stream()
                .anyMatch(grantedAuthority -> grantedAuthority.getAuthority().equals("ROLE_ADMIN"));

        if (isAdmin) {
            // El administrador visualiza todas las cuentas activas en la red
            List<Account> allAccounts = accountService.getAllAccounts();
            model.addAttribute("accounts", allAccounts);
            model.addAttribute("isAdmin", true);
        } else {
            // El cliente común visualiza únicamente sus cuentas propias filtradas por su email
            List<Account> userAccounts = accountService.getAccountsByUserEmail(loggedEmail);
            model.addAttribute("accounts", userAccounts);
            model.addAttribute("isAdmin", false);
        }

        return "accounts/list";
    }

    /**
     * Muestra el formulario para abrir una nueva cuenta bancaria.
     * Restringido únicamente para administradores (ADMIN).
     */
    @GetMapping("/new")
    @PreAuthorize("hasRole('ADMIN')")
    public String showOpenAccountForm(Model model) {
        model.addAttribute("account", new Account());

        // Permite al administrador seleccionar qué usuario/cliente será el titular de la cuenta
        // Cargamos la lista de todos los usuarios registrados con estado activo
        model.addAttribute("users", userService.searchUsers("", null).getContent());
        model.addAttribute("types", Account.AccountType.values());
        model.addAttribute("currencies", Account.Currency.values());

        return "accounts/form";
    }

    /**
     * Procesa la apertura y persistencia de una nueva cuenta bancaria.
     * Restringido únicamente para administradores (ADMIN).
     */
    @PostMapping("/new")
    @PreAuthorize("hasRole('ADMIN')")
    public String openAccount(
            @ModelAttribute("account") Account account,
            @RequestParam("userId") Long userId,
            RedirectAttributes redirectAttributes) {

        try {
            // Cargamos el usuario titular seleccionado
            User owner = userService.getUserById(userId);
            account.setUser(owner);
            account.setBalance(BigDecimal.ZERO); // Saldo inicial predeterminado en cero

            // Persistimos (JPA generará el IBAN aleatorio de manera automática en el @PrePersist)
            accountService.createAccount(account);

            redirectAttributes.addFlashAttribute("success",
                    "Cuenta abierta correctamente con titular: " + owner.getFirstName() + " " + owner.getLastName());
            return "redirect:/accounts";
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Error al abrir la cuenta: " + e.getMessage());
            return "redirect:/accounts/new";
        }
    }

    /**
     * Endpoint administrativo para suspender o reactivar una cuenta bancaria (Toggle Status).
     * Restringido únicamente para administradores (ADMIN).
     */
    @GetMapping("/toggle-status/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public String toggleAccountStatus(@PathVariable("id") Long id, RedirectAttributes redirectAttributes) {
        try {
            Account account = accountService.getAccountById(id);
            if (account.getStatus() == Account.AccountStatus.ACTIVE) {
                account.setStatus(Account.AccountStatus.SUSPENDED);
                redirectAttributes.addFlashAttribute("success",
                        "La cuenta con IBAN " + account.getIban() + " ha sido SUSPENDIDA (Bloqueada para transferencias).");
            } else {
                account.setStatus(Account.AccountStatus.ACTIVE);
                redirectAttributes.addFlashAttribute("success",
                        "La cuenta con IBAN " + account.getIban() + " ha sido ACTIVADA de inmediato.");
            }

            accountService.updateAccount(account);
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "No se pudo modificar el estado de la cuenta: " + e.getMessage());
        }

        return "redirect:/accounts";
    }
}