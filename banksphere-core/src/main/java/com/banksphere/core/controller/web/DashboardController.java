package com.banksphere.core.controller.web;

import com.banksphere.core.entity.Account;
import com.banksphere.core.entity.Transfer;
import com.banksphere.core.entity.User;
import com.banksphere.core.service.AccountService;
import com.banksphere.core.service.TransferService;
import com.banksphere.core.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.math.BigDecimal;
import java.security.Principal;
import java.util.List;

@Controller
public class DashboardController {

    private final UserService userService;
    private final AccountService accountService;
    private final TransferService transferService;

    @Autowired
    public DashboardController(UserService userService, AccountService accountService, TransferService transferService) {
        this.userService = userService;
        this.accountService = accountService;
        this.transferService = transferService;
    }

    @GetMapping("/dashboard")
    public String dashboard(Principal principal, Model model) {
        // 1. Identificar al usuario conectado
        User user = userService.getUserByEmail(principal.getName());

        // 2. Recuperar todas sus cuentas bancarias
        List<Account> accounts = accountService.getAccountsByUserEmail(user.getEmail());

        // 3. Calcular Saldo Total real (sumando cuentas activas)
        BigDecimal totalBalance = accounts.stream()
                .filter(a -> a.getStatus() == Account.AccountStatus.ACTIVE)
                .map(Account::getBalance)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // 4. Calcular operaciones reales (historial de transferencias)
        int totalTransfers = 0;
        for (Account acc : accounts) {
            List<Transfer> transfers = transferService.getMovementsByAccountId(acc.getId());
            totalTransfers += transfers.size();
        }

        // 5. Nivel de Riesgo (KYC)
        String riskScore = "Bajo (15%)";
        if (user.getKycDetail() != null) {
            switch (user.getKycDetail().getRiskLevel()) {
                case LOW: riskScore = "Bajo (15%)"; break;
                case MEDIUM: riskScore = "Medio (45%)"; break;
                case HIGH: riskScore = "Alto (85%)"; break;
            }
        } else if (user.getStatus() == User.UserStatus.PENDING_KYC) {
            riskScore = "No Evaluado";
        }

        // 6. Inyectar los datos en el HTML
        model.addAttribute("totalBalance", totalBalance);
        model.addAttribute("activeAccounts", accounts.size());
        model.addAttribute("totalTransfers", totalTransfers);
        model.addAttribute("riskScore", riskScore);

        return "dashboard/index";
    }
}