package com.bank.generator.service;

import com.bank.common.dto.contracts.exchange.ExchangeRateDTO;
import com.bank.generator.client.ExchangeClient;
import feign.FeignException;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public abstract class AbstractRateGeneratorService implements RateGeneratorService {
    protected final ExchangeClient exchangeClient;

    protected AbstractRateGeneratorService(ExchangeClient exchangeClient) {
        this.exchangeClient = exchangeClient;
    }

    protected void updateRate(String currency, Double buyRate, Double sellRate) {
        try {
            ExchangeRateDTO request = ExchangeRateDTO.builder()
                .currency(currency)
                .buyRate(buyRate)
                .sellRate(sellRate)
                .build();

            exchangeClient.updateRate(request);
            log.debug("Published exchange rate update: {} buy={} sell={}", currency, buyRate, sellRate);
        } catch (FeignException ex) {
            log.warn("Feign error while updating rate for {}: status={}, message={}",
                currency, ex.status(), ex.getMessage());
        } catch (Exception e) {
            log.error("Unexpected error updating rate for {}: {}", currency, e.getMessage(), e);
        }
    }
}
