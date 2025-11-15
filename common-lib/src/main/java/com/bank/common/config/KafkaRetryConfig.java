package com.bank.common.config;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.retrytopic.RetryTopicConfiguration;
import org.springframework.kafka.retrytopic.RetryTopicConfigurationBuilder;

/**
 * Kafka retry and DLT configuration with centralized DLT processing.
 */
@Configuration
@RequiredArgsConstructor
public class KafkaRetryConfig {

    private final KafkaProperties kafkaProperties;
    private final KafkaTemplate<String, Object> kafkaTemplate;

    @Bean
    public RetryTopicConfiguration retryTopicConfiguration() {
        KafkaProperties.Retry retry = kafkaProperties.getRetry();
        KafkaProperties.Retry.Backoff backoff = retry.getBackoff();

        RetryTopicConfigurationBuilder builder = RetryTopicConfigurationBuilder
                .newInstance()
                .maxAttempts(retry.getMaxAttempts())
                .exponentialBackoff(
                    backoff.getInitialInterval(),
                    backoff.getMultiplier(),
                    backoff.getMaxInterval()
                )
                .useSingleTopicForSameIntervals()
                .retryTopicSuffix(retry.getTopicSuffix())
                .dltSuffix(retry.getDltSuffix())
                .dltHandlerMethod("customDltProcessor", "processDltMessage")
                .autoStartDltHandler(retry.isAutoStartDltHandler());

        return builder.create(kafkaTemplate);
    }
}

