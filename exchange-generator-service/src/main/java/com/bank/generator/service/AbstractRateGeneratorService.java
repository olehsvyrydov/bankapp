package com.bank.generator.service;

import com.bank.common.dto.contracts.exchange.ExchangeRateDTO;
import com.bank.common.metrics.CustomMetricsService;
import com.bank.generator.kafka.ExchangeRateProducer;
import lombok.extern.slf4j.Slf4j;

import java.math.BigDecimal;

@Slf4j
public abstract class AbstractRateGeneratorService implements RateGeneratorService
{
    protected final ExchangeRateProducer exchangeRateProducer;
    protected final CustomMetricsService metricsService;

    protected AbstractRateGeneratorService(ExchangeRateProducer exchangeRateProducer, CustomMetricsService metricsService) {
        this.exchangeRateProducer = exchangeRateProducer;
        this.metricsService = metricsService;
    }

    protected void updateRate(String currency, BigDecimal buyRate, BigDecimal sellRate) {
        try {
            ExchangeRateDTO request = ExchangeRateDTO.builder()
                .currency(currency)
                .buyRate(buyRate)
                .sellRate(sellRate)
                .build();

            exchangeRateProducer.sendExchangeRate(request);
            metricsService.recordExchangeRateUpdate();
            log.debug("Published exchange rate update via Kafka: {} buy={} sell={}", currency, buyRate, sellRate);
        } catch (Exception e) {
            log.error("Unexpected error updating rate for {}: {}", currency, e.getMessage(), e);
            metricsService.recordExchangeRateUpdateFailure();
        }
    }
}
