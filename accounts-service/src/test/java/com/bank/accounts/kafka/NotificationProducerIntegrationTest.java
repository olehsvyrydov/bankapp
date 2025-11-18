package com.bank.accounts.kafka;

import com.bank.common.constants.KafkaTopics;
import com.bank.common.dto.contracts.notifications.NotificationRequest;
import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.support.serializer.JsonDeserializer;
import org.springframework.kafka.test.EmbeddedKafkaBroker;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.kafka.test.utils.KafkaTestUtils;
import org.springframework.test.annotation.DirtiesContext;

import java.time.Duration;
import java.util.Collections;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests for NotificationProducer with embedded Kafka.
 */
@SpringBootTest
@EmbeddedKafka(
    partitions = 1,
    topics = {KafkaTopics.NOTIFICATION_TOPIC},
    brokerProperties = {
        "auto.create.topics.enable=true"
    }
)
@DirtiesContext
class NotificationProducerIntegrationTest {

    @Autowired
    private NotificationProducer notificationProducer;

    @Autowired
    private EmbeddedKafkaBroker embeddedKafkaBroker;

    private Consumer<String, NotificationRequest> consumer;

    @BeforeEach
    void setUp() {
        Map<String, Object> consumerProps = KafkaTestUtils.consumerProps("test-group", "true", embeddedKafkaBroker);
        consumerProps.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        consumerProps.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, JsonDeserializer.class);
        consumerProps.put(JsonDeserializer.TRUSTED_PACKAGES, "*");
        consumerProps.put(JsonDeserializer.VALUE_DEFAULT_TYPE, NotificationRequest.class.getName());

        consumer = new DefaultKafkaConsumerFactory<String, NotificationRequest>(consumerProps).createConsumer();
        consumer.subscribe(Collections.singletonList(KafkaTopics.NOTIFICATION_TOPIC));
    }

    @AfterEach
    void tearDown() {
        if (consumer != null) {
            consumer.close();
        }
    }

    @Test
    void shouldSendNotificationToKafkaTopic() {
        NotificationRequest notification = NotificationRequest.builder()
            .username("testuser")
            .message("Test notification message")
            .type("INFO")
            .build();

        notificationProducer.sendNotification(notification);

        ConsumerRecord<String, NotificationRequest> received = KafkaTestUtils.getSingleRecord(consumer, KafkaTopics.NOTIFICATION_TOPIC, Duration.ofSeconds(10));

        assertThat(received).isNotNull();
        assertThat(received.key()).isEqualTo("testuser");
        assertThat(received.value()).isNotNull();
        assertThat(received.value().getUsername()).isEqualTo("testuser");
        assertThat(received.value().getMessage()).isEqualTo("Test notification message");
        assertThat(received.value().getType()).isEqualTo("INFO");
    }

    @Test
    void shouldUseUsernameAsPartitionKey() {
        NotificationRequest notification1 = NotificationRequest.builder()
            .username("user1")
            .message("Message 1")
            .type("INFO")
            .build();

        NotificationRequest notification2 = NotificationRequest.builder()
            .username("user1")
            .message("Message 2")
            .type("INFO")
            .build();

        notificationProducer.sendNotification(notification1);
        notificationProducer.sendNotification(notification2);

        var records = KafkaTestUtils.getRecords(consumer, Duration.ofSeconds(10));

        assertThat(records.count()).isEqualTo(2);

        var recordsList = new java.util.ArrayList<ConsumerRecord<String, NotificationRequest>>();
        records.forEach(recordsList::add);

        assertThat(recordsList).hasSize(2);
        assertThat(recordsList.get(0).key()).isEqualTo("user1");
        assertThat(recordsList.get(1).key()).isEqualTo("user1");
        assertThat(recordsList.get(0).partition()).isEqualTo(recordsList.get(1).partition());
    }
}
