package com.bank.frontend.controller;

import com.bank.common.dto.contracts.accounts.AccountDTO;
import com.bank.common.dto.contracts.accounts.BankAccountDTO;
import com.bank.common.dto.contracts.exchange.ExchangeRateDTO;
import com.bank.frontend.exceptions.UnauthorizedException;
import com.bank.frontend.service.AccountServiceClient;
import com.bank.frontend.service.ExchangeServiceClient;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.util.*;

@Controller
@Slf4j
@RequiredArgsConstructor
public class HomeController {

    private final AccountServiceClient accountServiceClient;
    private final ExchangeServiceClient exchangeServiceClient;

    @GetMapping("/home")
    public String home(HttpSession session, Model model) {
        String username = (String) session.getAttribute("username");
        String token = (String) session.getAttribute("access_token");

        log.debug("Accessing home page. Username: {}, Token present: {}", username, token != null);

        if (username == null || token == null) {
            log.debug("No valid session, redirecting to login");
            return "redirect:/login";
        }

        log.debug("User {} accessing home page", username);

        AccountDTO accountDTO;
        try {
            accountDTO = accountServiceClient.getAccountDetails();
        } catch (UnauthorizedException ex) {
            log.warn("Session expired for user {}, redirecting to login", username);
            session.invalidate();
            return "redirect:/login?sessionExpired=true";
        }

        Map<String, Object> account = new java.util.HashMap<>();
        account.put("username", username);
        account.put("firstName", accountDTO.getFirstName());
        account.put("lastName", accountDTO.getLastName());
        account.put("email", accountDTO.getEmail());
        account.put("birthDate", accountDTO.getBirthDate() != null ? accountDTO.getBirthDate().toString() : "");
        account.put("bankAccounts", accountDTO.getBankAccounts());

        Set<String> existingCurrencies = new HashSet<>();
        if (accountDTO.getBankAccounts() != null) {
            for (BankAccountDTO ba : accountDTO.getBankAccounts()) {
                if (ba.getCurrency() != null) {
                    existingCurrencies.add(ba.getCurrency());
                }
            }
        }

        List<String> supportedCurrencies = List.of("RUB", "USD", "CNY");
        List<String> availableCurrencies = supportedCurrencies.stream()
            .filter(currency -> !existingCurrencies.contains(currency))
            .toList();

        List<ExchangeRateDTO> exchangeRates = new ArrayList<>();
        try {
            exchangeRates = exchangeServiceClient.getExchangeRates().stream()
                .filter(cur -> !cur.getCurrency().equals("RUB")).toList();
        } catch (Exception e) {
            log.warn("Failed to fetch exchange rates: {}", e.getMessage());
        }

        model.addAttribute("account", account);
        model.addAttribute("username", username);
        model.addAttribute("availableCurrencies", availableCurrencies);
        model.addAttribute("notifications", new ArrayList<>());
        model.addAttribute("exchangeRates", exchangeRates);

        return "home";
    }
}
