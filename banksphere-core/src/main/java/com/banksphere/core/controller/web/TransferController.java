package com.banksphere.core.controller.web;

import com.banksphere.core.entity.Account;
import com.banksphere.core.entity.Transfer;
import com.banksphere.core.entity.User;
import com.banksphere.core.service.AccountService;
import com.banksphere.core.service.TransferService;
import com.banksphere.core.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Controller
@RequestMapping("/transfers")
public class TransferController {

    private final TransferService transferService;
    private final AccountService accountService;
    private final UserService userService;

    @Autowired
    public TransferController(TransferService transferService,
                              AccountService accountService,
                              UserService userService) {
        this.transferService = transferService;
        this.accountService = accountService;
        this.userService = userService;
    }

    /**
     * Muestra el historial consolidad de transferencias asociadas a las cuentas del usuario.
     */
    @GetMapping
    public String listTransfers(Model model, Authentication authentication) {
        String loggedEmail = authentication.getName();
        List<Account> userAccounts = accountService.getAccountsByUserEmail(loggedEmail);

        List<Transfer> movements = new ArrayList<>();
        for (Account account : userAccounts) {
            movements.addAll(transferService.getMovementsByAccountId(account.getId()));
        }

        // Ordenamos las transferencias por fecha (la consulta JPA ya suele ordenarlas, pero consolidamos)
        movements.sort((t1, t2) -> t2.getCreatedAt().compareTo(t1.getCreatedAt()));

        model.addAttribute("accounts", userAccounts);
        model.addAttribute("movements", movements);
        return "transfers/history";
    }

    @GetMapping("/new")
    public String showTransferForm(@RequestParam(value = "source", required = false) String paramSource,
                                   Model model,
                                   Authentication authentication) {
        String loggedEmail = authentication.getName();
        List<Account> userAccounts = accountService.getAccountsByUserEmail(loggedEmail);

        model.addAttribute("accounts", userAccounts);
        model.addAttribute("paramSource", paramSource);
        return "transfers/new";
    }

    @PostMapping("/new")
    public String executeTransfer(@RequestParam("sourceIban") String sourceIban,
                                  @RequestParam("destinationIban") String destinationIban,
                                  @RequestParam("amount") BigDecimal amount,
                                  @RequestParam("transferType") Transfer.TransferType transferType,
                                  @RequestParam(value = "concept", required = false) String concept,
                                  RedirectAttributes redirectAttributes) {
        try {
            transferService.executeTransfer(sourceIban, destinationIban, amount, transferType, concept);
            redirectAttributes.addFlashAttribute("success", "Transferencia emitida con éxito. Los fondos han sido debitados y enviados a la red.");
            return "redirect:/transfers";
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Error en la transacción: " + e.getMessage());
            return "redirect:/transfers/new?source=" + sourceIban;
        }
    }
}