package com.bank.exchange.config;

import com.bank.common.config.KafkaProperties;
import com.bank.common.dto.contracts.exchange.ExchangeRateDTO;
import lombok.RequiredArgsConstructor;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.annotation.EnableKafka;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.support.serializer.ErrorHandlingDeserializer;
import org.springframework.kafka.support.serializer.JsonDeserializer;

import java.util.HashMap;
import java.util.Map;

/**
 * Kafka consumer configuration for exchange rate updates.
 * Configured for "at most once" delivery with auto-commit enabled.
 */
@Configuration
@RequiredArgsConstructor
public class ExchangeKafkaConsumerConfig {

    private final KafkaProperties kafkaProperties;

    @Bean
    public ConsumerFactory<String, ExchangeRateDTO> exchangeConsumerFactory() {
        Map<String, Object> props = new HashMap<>();
        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, kafkaProperties.getBootstrapServers());
        props.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, true);
        props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "latest");

        // Configure JsonDeserializer properties
        // FIX: Set VALUE_DEFAULT_TYPE to ensure deserialization works even without type headers
        props.put(JsonDeserializer.VALUE_DEFAULT_TYPE, ExchangeRateDTO.class.getName());
        props.put(JsonDeserializer.TRUSTED_PACKAGES, "com.bank.*");
        props.put(JsonDeserializer.USE_TYPE_INFO_HEADERS, false);

        // Create and configure deserializers
        JsonDeserializer<ExchangeRateDTO> valueDeserializer = new JsonDeserializer<>();
        valueDeserializer.configure(props, false);

        ErrorHandlingDeserializer<ExchangeRateDTO> errorHandlingDeserializer =
            new ErrorHandlingDeserializer<>(valueDeserializer);
        errorHandlingDeserializer.configure(props, false);

        return new DefaultKafkaConsumerFactory<>(
            props,
            new StringDeserializer(),
            errorHandlingDeserializer
        );
    }

    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, ExchangeRateDTO> exchangeKafkaListenerContainerFactory() {
        ConcurrentKafkaListenerContainerFactory<String, ExchangeRateDTO> factory =
            new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(exchangeConsumerFactory());
        return factory;
    }
}
