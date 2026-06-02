package com.banksphere.core.controller.api;

import com.banksphere.common.dto.AccountDTO;
import com.banksphere.core.entity.Account;
import com.banksphere.core.entity.User;
import com.banksphere.core.service.AccountService;
import com.banksphere.core.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1/openbanking/accounts")
@Tag(name = "Accounts & Balances API", description = "Endpoints de Open Banking para la gestión y consulta de cuentas bancarias")
public class AccountRestController {

    private final AccountService accountService;
    private final UserService userService;

    @Autowired
    public AccountRestController(AccountService accountService, UserService userService) {
        this.accountService = accountService;
        this.userService = userService;
    }

    @GetMapping
    @Operation(summary = "Obtener todas las cuentas", description = "Lista la totalidad de las cuentas bancarias registradas en la red")
    public ResponseEntity<List<AccountDTO>> getAllAccounts() {
        List<AccountDTO> dtos = accountService.getAllAccounts().stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
        return ResponseEntity.ok(dtos);
    }

    @GetMapping("/{iban}")
    @Operation(summary = "Obtener cuenta por IBAN", description = "Recupera el detalle de una cuenta bancaria a partir de su IBAN único")
    @ApiResponse(responseCode = "200", description = "Cuenta encontrada con éxito")
    @ApiResponse(responseCode = "404", description = "Cuenta no encontrada")
    public ResponseEntity<AccountDTO> getAccountByIban(
            @Parameter(description = "IBAN de la cuenta a buscar", required = true)
            @PathVariable("iban") String iban) {
        try {
            Account account = accountService.getAccountByIban(iban);
            return ResponseEntity.ok(convertToDTO(account));
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @GetMapping("/user/{userId}")
    @Operation(summary = "Obtener cuentas de un usuario", description = "Lista todas las cuentas asociadas a un ID de usuario/titular específico")
    public ResponseEntity<List<AccountDTO>> getAccountsByUserId(@PathVariable("userId") Long userId) {
        try {
            User user = userService.getUserById(userId);
            List<AccountDTO> dtos = accountService.getAccountsByUserEmail(user.getEmail()).stream()
                    .map(this::convertToDTO)
                    .collect(Collectors.toList());
            return ResponseEntity.ok(dtos);
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @GetMapping("/{iban}/balance")
    @Operation(summary = "Consultar balance de cuenta", description = "Devuelve exclusivamente el saldo actual y la moneda de la cuenta indicada")
    public ResponseEntity<BigDecimal> getAccountBalance(@PathVariable("iban") String iban) {
        try {
            Account account = accountService.getAccountByIban(iban);
            return ResponseEntity.ok(account.getBalance());
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @PostMapping
    @Operation(summary = "Abrir una cuenta bancaria", description = "Abre una nueva cuenta (SAVINGS, CHECKING, BUSINESS) asociada a un titular")
    @ApiResponse(responseCode = "201", description = "Cuenta abierta con éxito")
    @ApiResponse(responseCode = "400", description = "Petición inválida o error en validaciones")
    public ResponseEntity<?> openAccount(@Valid @RequestBody AccountDTO accountDTO) {
        try {
            User owner = userService.getUserById(accountDTO.getUserId());

            Account account = Account.builder()
                    .accountType(Account.AccountType.valueOf(accountDTO.getAccountType().toUpperCase()))
                    .currency(Account.Currency.valueOf(accountDTO.getCurrency().toUpperCase()))
                    .user(owner)
                    .build();

            Account savedAccount = accountService.createAccount(account);
            return ResponseEntity.status(HttpStatus.CREATED).body(convertToDTO(savedAccount));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(e.getMessage());
        }
    }

    // Helper method to convert Entity to DTO
    private AccountDTO convertToDTO(Account account) {
        return AccountDTO.builder()
                .id(account.getId())
                .iban(account.getIban())
                .accountType(account.getAccountType().name())
                .balance(account.getBalance())
                .currency(account.getCurrency().name())
                .status(account.getStatus().name())
                .userId(account.getUser().getId())
                .createdAt(account.getCreatedAt())
                .build();
    }
}