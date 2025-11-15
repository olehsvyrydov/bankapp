package com.bank.generator.service;

import com.bank.common.dto.contracts.exchange.ExchangeRateDTO;
import com.bank.generator.kafka.ExchangeRateProducer;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.client.discovery.DiscoveryClient;

import java.math.BigDecimal;

@Slf4j
public abstract class AbstractRateGeneratorService implements RateGeneratorService
{
    protected static final String TARGET_SERVICE_ID = "exchange-service";
    private boolean awaitingGatewayLogPrinted = false;

    protected final ExchangeRateProducer exchangeRateProducer;
    protected final DiscoveryClient discoveryClient;

    protected AbstractRateGeneratorService(ExchangeRateProducer exchangeRateProducer, DiscoveryClient discoveryClient) {
        this.exchangeRateProducer = exchangeRateProducer;
        this.discoveryClient = discoveryClient;
    }

    protected void updateRate(String currency, BigDecimal buyRate, BigDecimal sellRate) {
        try {
            ExchangeRateDTO request = ExchangeRateDTO.builder()
                .currency(currency)
                .buyRate(buyRate)
                .sellRate(sellRate)
                .build();

            exchangeRateProducer.sendExchangeRate(request);
            log.debug("Published exchange rate update via Kafka: {} buy={} sell={}", currency, buyRate, sellRate);
        } catch (Exception e) {
            log.error("Unexpected error updating rate for {}: {}", currency, e.getMessage(), e);
        }
    }

    protected boolean isTargetServiceAvailable() {
        boolean available = !discoveryClient.getInstances(TARGET_SERVICE_ID).isEmpty();
        if (!available) {
            if (!awaitingGatewayLogPrinted) {
                log.info("Waiting for {} to register in discovery before publishing exchange rates", TARGET_SERVICE_ID);
                awaitingGatewayLogPrinted = true;
            }
            return false;
        }

        if (awaitingGatewayLogPrinted) {
            log.info("{} discovered. Exchange rate publishing is now active", TARGET_SERVICE_ID);
            awaitingGatewayLogPrinted = false;
        }
        return true;
    }
}
