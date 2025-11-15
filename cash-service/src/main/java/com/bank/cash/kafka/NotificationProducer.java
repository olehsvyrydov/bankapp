package com.bank.cash.kafka;

import com.bank.common.constants.KafkaTopics;
import com.bank.common.dto.contracts.notifications.NotificationRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Component;

import java.util.concurrent.CompletableFuture;

/**
 * Kafka producer for sending notification messages.
 * Implements "at least once" delivery semantics with acknowledgment.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class NotificationProducer {

    private final KafkaTemplate<String, Object> kafkaTemplate;

    /**
     * Sends notification message to Kafka topic.
     * Uses username as partition key for ordered delivery per user.
     *
     * @param notification the notification to send
     */
    public void sendNotification(NotificationRequest notification) {
        log.info("Sending notification to Kafka topic: {} for user: {}",
            KafkaTopics.NOTIFICATION_TOPIC, notification.getUsername());

        CompletableFuture<SendResult<String, Object>> future = kafkaTemplate.send(
            KafkaTopics.NOTIFICATION_TOPIC,
            notification.getUsername(),
            notification
        );

        future.whenComplete((result, ex) -> {
            if (ex == null) {
                log.info("Notification sent successfully for user: {} to partition: {} with offset: {}",
                    notification.getUsername(),
                    result.getRecordMetadata().partition(),
                    result.getRecordMetadata().offset());
            } else {
                log.error("Failed to send notification for user: {}", notification.getUsername(), ex);
            }
        });
    }
}
