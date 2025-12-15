package com.bank.common.kafka;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.MessageBuilder;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for CustomDltProcessor.
 */
class CustomDltProcessorTest {

    private CustomDltProcessor customDltProcessor;
    private ListAppender<ILoggingEvent> logAppender;

    @BeforeEach
    void setUp() {
        customDltProcessor = new CustomDltProcessor();

        Logger logger = (Logger) LoggerFactory.getLogger(CustomDltProcessor.class);
        logAppender = new ListAppender<>();
        logAppender.start();
        logger.addAppender(logAppender);
    }

    @Test
    void shouldLogDltMessageWithCorrectPrefix() {
        Message<String> message = MessageBuilder
            .withPayload("test payload")
            .setHeader(KafkaHeaders.RECEIVED_TOPIC, "test-topic-dlt")
            .setHeader(KafkaHeaders.RECEIVED_PARTITION, 0)
            .setHeader(KafkaHeaders.OFFSET, 123L)
            .setHeader(KafkaHeaders.EXCEPTION_MESSAGE, "Test exception message")
            .build();

        customDltProcessor.processDltMessage(message);

        assertThat(logAppender.list).hasSize(1);
        ILoggingEvent logEvent = logAppender.list.get(0);

        assertThat(logEvent.getLevel().toString()).isEqualTo("WARN");
        assertThat(logEvent.getFormattedMessage()).startsWith("DLT_MESSAGE:");
        assertThat(logEvent.getFormattedMessage()).contains("topic=test-topic-dlt");
        assertThat(logEvent.getFormattedMessage()).contains("partition=0");
        assertThat(logEvent.getFormattedMessage()).contains("offset=123");
        assertThat(logEvent.getFormattedMessage()).contains("payload=test payload");
        assertThat(logEvent.getFormattedMessage()).contains("error=Test exception message");
    }

    @Test
    void shouldHandleNullHeaders() {
        Message<String> message = MessageBuilder
            .withPayload("test payload")
            .build();

        customDltProcessor.processDltMessage(message);

        assertThat(logAppender.list).hasSize(1);
        ILoggingEvent logEvent = logAppender.list.get(0);

        assertThat(logEvent.getLevel().toString()).isEqualTo("WARN");
        assertThat(logEvent.getFormattedMessage()).startsWith("DLT_MESSAGE:");
    }

    @Test
    void shouldLogComplexPayload() {
        TestPayload payload = new TestPayload("user123", "Test message");

        Message<TestPayload> message = MessageBuilder
            .withPayload(payload)
            .setHeader(KafkaHeaders.RECEIVED_TOPIC, "notifications-dlt")
            .setHeader(KafkaHeaders.RECEIVED_PARTITION, 1)
            .setHeader(KafkaHeaders.OFFSET, 456L)
            .setHeader(KafkaHeaders.EXCEPTION_MESSAGE, "Processing failed")
            .build();

        customDltProcessor.processDltMessage(message);

        assertThat(logAppender.list).hasSize(1);
        ILoggingEvent logEvent = logAppender.list.get(0);

        assertThat(logEvent.getFormattedMessage()).contains("topic=notifications-dlt");
        assertThat(logEvent.getFormattedMessage()).contains("error=Processing failed");
    }

    private static class TestPayload {
        private final String username;
        private final String message;

        TestPayload(String username, String message) {
            this.username = username;
            this.message = message;
        }

        @Override
        public String toString() {
            return "TestPayload{username='" + username + "', message='" + message + "'}";
        }
    }
}
