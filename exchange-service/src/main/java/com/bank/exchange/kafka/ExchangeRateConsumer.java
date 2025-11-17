package com.bank.exchange.kafka;

import com.bank.common.constants.KafkaTopics;
import com.bank.common.dto.contracts.exchange.ExchangeRateDTO;
import com.bank.exchange.service.ExchangeService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;



/**
 * Kafka consumer for processing exchange rate updates.
 * Implements "at most once" delivery semantics for real-time rate updates.
 * Starts from latest messages on restart to avoid processing stale rates.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ExchangeRateConsumer {

    private final ExchangeService exchangeService;

    /**
     * Consumes exchange rate updates from Kafka topic.
     * Updates are processed in order per currency due to partition key strategy.
     *
     * @param exchangeRate the exchange rate update
     * @param topic the topic from which the message was consumed
     * @param partition the partition number
     * @param offset the message offset
     */
    @KafkaListener(
        topics = KafkaTopics.EXCHANGE_RATE_TOPIC,
        groupId = "${spring.kafka.consumer.group-id:exchange-consumer-group}",
        containerFactory = "exchangeKafkaListenerContainerFactory"
    )
    public void consumeExchangeRate(
        @Payload ExchangeRateDTO exchangeRate,
        @Header(KafkaHeaders.RECEIVED_TOPIC) String topic,
        @Header(KafkaHeaders.RECEIVED_PARTITION) int partition,
        @Header(KafkaHeaders.OFFSET) long offset
    ) {
        // FIX: Handle missing timestamp header (common in retry topics)
        log.debug("Consumed exchange rate. Topic: {}, partition: {}, offset: {}, currency: {}",
                topic, partition, offset, exchangeRate.getCurrency());

        try {
            exchangeService.updateRate(
                exchangeRate.getCurrency(),
                exchangeRate.getBuyRate(),
                exchangeRate.getSellRate()
            );
            log.debug("Successfully updated exchange rate for {}: buy={}, sell={}",
                exchangeRate.getCurrency(), exchangeRate.getBuyRate(), exchangeRate.getSellRate());
        } catch (Exception ex) {
            log.error("Error updating exchange rate for {}", exchangeRate.getCurrency(), ex);
        }
    }
}
