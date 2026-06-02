package com.banksphere.core.controller.api;

import com.banksphere.common.dto.TransferDTO;
import com.banksphere.core.entity.Account;
import com.banksphere.core.entity.Transfer;
import com.banksphere.core.service.AccountService;
import com.banksphere.core.service.TransferService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1/openbanking/transfers")
@Tag(name = "Transfers API", description = "Endpoints de Open Banking para emitir y auditar transferencias monetarias")
public class TransferRestController {

    private final TransferService transferService;
    private final AccountService accountService;

    @Autowired
    public TransferRestController(TransferService transferService, AccountService accountService) {
        this.transferService = transferService;
        this.accountService = accountService;
    }

    @GetMapping("/account/{iban}")
    @Operation(summary = "Historial de transferencias por cuenta", description = "Recupera todas las transferencias enviadas o recibidas por una cuenta (IBAN)")
    public ResponseEntity<?> getMovementsByAccount(@PathVariable("iban") String iban) {
        try {
            Account account = accountService.getAccountByIban(iban);
            List<TransferDTO> dtos = transferService.getMovementsByAccountId(account.getId()).stream()
                    .map(this::convertToDTO)
                    .collect(Collectors.toList());
            return ResponseEntity.ok(dtos);
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Cuenta no encontrada con el IBAN: " + iban);
        }
    }

    @PostMapping
    @Operation(summary = "Emitir una transferencia", description = "Realiza una transferencia monetaria entre dos cuentas aplicando validaciones de fondos, estado y comisiones automáticas")
    @ApiResponse(responseCode = "201", description = "Transferencia ejecutada con éxito")
    @ApiResponse(responseCode = "400", description = "Fondos insuficientes, cuentas suspendidas o datos incorrectos")
    public ResponseEntity<?> executeTransfer(@Valid @RequestBody TransferDTO transferDTO) {
        try {
            Transfer.TransferType type = Transfer.TransferType.valueOf(transferDTO.getTransferType().toUpperCase());

            Transfer result = transferService.executeTransfer(
                    transferDTO.getSourceIban(),
                    transferDTO.getDestinationIban(),
                    transferDTO.getAmount(),
                    type,
                    transferDTO.getConcept()
            );

            return ResponseEntity.status(HttpStatus.CREATED).body(convertToDTO(result));
        } catch (IllegalArgumentException | IllegalStateException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
        }
    }

    // Helper method to convert Entity to DTO
    private TransferDTO convertToDTO(Transfer transfer) {
        return TransferDTO.builder()
                .id(transfer.getId())
                .sourceIban(transfer.getSourceAccount().getIban())
                .destinationIban(transfer.getDestinationAccount().getIban())
                .amount(transfer.getAmount())
                .fee(transfer.getFee())
                .concept(transfer.getConcept())
                .transferType(transfer.getTransferType().name())
                .status(transfer.getStatus().name())
                .createdAt(transfer.getCreatedAt())
                .build();
    }
}