package com.bank.common.constants;

public final class KafkaTopics {

    // Notification topics
    public static final String NOTIFICATION_TOPIC = "bank.notifications";

    // Exchange rate topics
    public static final String EXCHANGE_RATE_TOPIC = "bank.exchange.rates";

    private KafkaTopics() {
        // Prevent instantiation
    }
}
