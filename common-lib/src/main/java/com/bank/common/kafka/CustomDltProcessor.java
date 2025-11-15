package com.bank.common.kafka;

import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.Message;
import org.springframework.stereotype.Component;

/**
 * Centralized Dead Letter Topic (DLT) processor for handling failed Kafka messages.
 * Logs DLT messages with DLT_MESSAGE prefix for monitoring system collection.
 */
@Slf4j
@Component
public class CustomDltProcessor {

    /**
     * Processes messages that failed all retry attempts and moved to DLT.
     * Logs structured message for monitoring systems (Kibana/Prometheus).
     *
     * @param message the failed message
     */
    public void processDltMessage(Message<?> message) {
        String topic = message.getHeaders().get(KafkaHeaders.RECEIVED_TOPIC, String.class);
        Integer partition = message.getHeaders().get(KafkaHeaders.RECEIVED_PARTITION, Integer.class);
        Long offset = message.getHeaders().get(KafkaHeaders.OFFSET, Long.class);
        String exceptionMessage = message.getHeaders().get(KafkaHeaders.EXCEPTION_MESSAGE, String.class);

        Object payload = message.getPayload();

        log.warn("DLT_MESSAGE: topic={}, partition={}, offset={}, payload={}, error={}",
            topic, partition, offset, payload, exceptionMessage);
    }
}
