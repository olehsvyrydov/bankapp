package com.bank.generator.kafka;

import com.bank.common.constants.KafkaTopics;
import com.bank.common.dto.contracts.exchange.ExchangeRateDTO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

/**
 * Kafka producer for sending exchange rate updates.
 * Implements "at most once" delivery semantics for real-time rate updates.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ExchangeRateProducer {

    private final KafkaTemplate<String, Object> kafkaTemplate;

    /**
     * Sends exchange rate update to Kafka topic.
     * Uses currency as partition key to ensure ordered delivery per currency.
     * Fire-and-forget approach for "at most once" semantics.
     *
     * @param exchangeRate the exchange rate to send
     */
    public void sendExchangeRate(ExchangeRateDTO exchangeRate) {
        String currency = exchangeRate.getCurrency();
        log.debug("Sending exchange rate to Kafka for currency: {}", currency);

        try {
            kafkaTemplate.send(KafkaTopics.EXCHANGE_RATE_TOPIC, currency, exchangeRate);
            log.debug("Exchange rate sent for {}: buy={}, sell={}",
                currency, exchangeRate.getBuyRate(), exchangeRate.getSellRate());
        } catch (Exception ex) {
            log.warn("Failed to send exchange rate for {}, continuing (at-most-once delivery)", currency);
        }
    }
}
