package com.bank.frontend.controller;

import com.bank.common.dto.contracts.exchange.ExchangeRateDTO;
import com.bank.common.util.ErrorMessageUtil;
import com.bank.frontend.service.ExchangeServiceClient;
import com.bank.frontend.service.LocalizationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * REST API controller for exchange rates operations.
 */
@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
@Slf4j
public class ExchangeRatesController {

    private final ExchangeServiceClient exchangeServiceClient;
    private final LocalizationService localizationService;

    /**
     * Retrieves current exchange rates for all supported currencies.
     * Filters out RUB (base currency) as it's always 1.0000 and doesn't need to be displayed.
     *
     * @return response entity with list of exchange rates or error
     */
    @GetMapping("/exchange/rates")
    public ResponseEntity<?> getExchangeRates() {
        log.debug("Fetching exchange rates");

        try {
            List<ExchangeRateDTO> rates = exchangeServiceClient.getExchangeRates();

            // Filter out RUB (base currency) - it's always 1.0000 and doesn't make sense to show
            List<ExchangeRateDTO> filteredRates = rates.stream()
                .filter(rate -> !"RUB".equalsIgnoreCase(rate.getCurrency()))
                .toList();

            log.info("Successfully fetched {} exchange rates (filtered {} total, showing {} without RUB)",
                filteredRates.size(), rates.size(), filteredRates.size());
            return ResponseEntity.ok(filteredRates);
        } catch (Exception e) {
            log.error("Failed to fetch exchange rates: {}",
                ErrorMessageUtil.sanitizeForLogging(e.getMessage()));
            String defaultMessage = localizationService.getMessage("exchange.fetch.error");
            String friendlyMessage = ErrorMessageUtil.extractUserFriendlyMessage(
                e.getMessage(),
                defaultMessage
            );
            return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of(
                    "error", friendlyMessage,
                    "rates", Collections.emptyList()
                ));
        }
    }
}
