package com.bank.notifications.kafka;

import com.bank.common.constants.KafkaTopics;
import com.bank.common.dto.contracts.notifications.NotificationRequest;
import com.bank.notifications.service.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.annotation.RetryableTopic;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.retry.annotation.Backoff;
import org.springframework.stereotype.Component;

/**
 * Kafka consumer for processing notification messages.
 * Implements retry mechanism with exponential backoff.
 * DLT handling is centralized in common-lib CustomDltProcessor.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class NotificationConsumer {

    private final NotificationService notificationService;

    /**
     * Consumes notification messages from Kafka topic with retry support.
     * Failed messages will be retried according to configured retry policy.
     * Messages that fail all retries are automatically sent to DLT and processed by CustomDltProcessor.
     *
     * @param notification the notification request to process
     * @param topic the topic from which the message was consumed
     * @param partition the partition number
     * @param offset the message offset
     */
    @RetryableTopic(
        attempts = "${spring.kafka.retry.max-attempts:5}",
        backoff = @Backoff(
            delayExpression = "${spring.kafka.retry.backoff.initial-interval:1000}",
            multiplierExpression = "${spring.kafka.retry.backoff.multiplier:2.0}",
            maxDelayExpression = "${spring.kafka.retry.backoff.max-interval:10000}"
        ),
        retryTopicSuffix = "${spring.kafka.retry.topic-suffix:-retry}",
        dltTopicSuffix = "${spring.kafka.retry.dlt-suffix:-dlt}",
        include = {Exception.class},
        autoCreateTopics = "true"
    )
    @KafkaListener(
        topics = KafkaTopics.NOTIFICATION_TOPIC,
        groupId = "${spring.kafka.consumer.group-id:notifications-consumer-group}",
        containerFactory = "kafkaListenerContainerFactory"
    )
    public void consumeNotification(
        @Payload NotificationRequest notification,
        @Header(KafkaHeaders.RECEIVED_TOPIC) String topic,
        @Header(KafkaHeaders.RECEIVED_PARTITION) int partition,
        @Header(KafkaHeaders.OFFSET) long offset
    ) {
        log.info("Consumed notification from topic: {}, partition: {}, offset: {}, username: {}",
            topic, partition, offset, notification.getUsername());

        try {
            notificationService.sendNotification(notification);
            log.info("Successfully processed notification for user: {}", notification.getUsername());
        } catch (Exception ex) {
            log.error("Error processing notification for user: {}", notification.getUsername(), ex);
            throw ex;
        }
    }
}
