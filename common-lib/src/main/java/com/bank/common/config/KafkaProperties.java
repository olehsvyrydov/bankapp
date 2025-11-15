package com.bank.common.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Data
@Configuration
@ConfigurationProperties(prefix = "spring.kafka")
public class KafkaProperties {

    private String bootstrapServers = "localhost:9092";
    private Retry retry = new Retry();

    @Data
    public static class Retry {
        private int maxAttempts = 5;
        private String topicSuffix = "-retry";
        private String dltSuffix = "-dlt";
        private boolean autoStartDltHandler = true;
        private Backoff backoff = new Backoff();

        @Data
        public static class Backoff {
            private long initialInterval = 1000L;
            private double multiplier = 2.0;
            private long maxInterval = 10000L;
        }
    }
}
