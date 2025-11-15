package com.bank.generator.kafka;

import com.bank.common.constants.KafkaTopics;
import com.bank.common.dto.contracts.exchange.ExchangeRateDTO;
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

import java.math.BigDecimal;
import java.time.Duration;
import java.util.Collections;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests for ExchangeRateProducer with embedded Kafka.
 */
@SpringBootTest
@EmbeddedKafka(
    partitions = 1,
    topics = {KafkaTopics.EXCHANGE_RATE_TOPIC},
    brokerProperties = {
        "auto.create.topics.enable=true"
    }
)
@DirtiesContext
class ExchangeRateProducerIntegrationTest {

    @Autowired
    private ExchangeRateProducer exchangeRateProducer;

    @Autowired
    private EmbeddedKafkaBroker embeddedKafkaBroker;

    private Consumer<String, ExchangeRateDTO> consumer;

    @BeforeEach
    void setUp() {
        Map<String, Object> consumerProps = KafkaTestUtils.consumerProps("test-group", "true", embeddedKafkaBroker);
        consumerProps.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        consumerProps.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, JsonDeserializer.class);
        consumerProps.put(JsonDeserializer.TRUSTED_PACKAGES, "*");
        consumerProps.put(JsonDeserializer.VALUE_DEFAULT_TYPE, ExchangeRateDTO.class.getName());

        consumer = new DefaultKafkaConsumerFactory<String, ExchangeRateDTO>(consumerProps).createConsumer();
        consumer.subscribe(Collections.singletonList(KafkaTopics.EXCHANGE_RATE_TOPIC));
    }

    @AfterEach
    void tearDown() {
        if (consumer != null) {
            consumer.close();
        }
    }

    @Test
    void shouldSendExchangeRateToKafkaTopic() {
        ExchangeRateDTO exchangeRate = ExchangeRateDTO.builder()
            .currency("USD")
            .buyRate(new BigDecimal("75.50"))
            .sellRate(new BigDecimal("76.50"))
            .build();

        exchangeRateProducer.sendExchangeRate(exchangeRate);

        ConsumerRecord<String, ExchangeRateDTO> received = KafkaTestUtils.getSingleRecord(consumer, KafkaTopics.EXCHANGE_RATE_TOPIC, Duration.ofSeconds(10));

        assertThat(received).isNotNull();
        assertThat(received.key()).isEqualTo("USD");
        assertThat(received.value()).isNotNull();
        assertThat(received.value().getCurrency()).isEqualTo("USD");
        assertThat(received.value().getBuyRate()).isEqualByComparingTo(new BigDecimal("75.50"));
        assertThat(received.value().getSellRate()).isEqualByComparingTo(new BigDecimal("76.50"));
    }

    @Test
    void shouldUseCurrencyAsPartitionKey() {
        ExchangeRateDTO usdRate1 = ExchangeRateDTO.builder()
            .currency("USD")
            .buyRate(new BigDecimal("75.50"))
            .sellRate(new BigDecimal("76.50"))
            .build();

        ExchangeRateDTO usdRate2 = ExchangeRateDTO.builder()
            .currency("USD")
            .buyRate(new BigDecimal("75.60"))
            .sellRate(new BigDecimal("76.60"))
            .build();

        exchangeRateProducer.sendExchangeRate(usdRate1);
        exchangeRateProducer.sendExchangeRate(usdRate2);

        var records = KafkaTestUtils.getRecords(consumer, Duration.ofSeconds(10));

        assertThat(records.count()).isEqualTo(2);

        var recordsList = new java.util.ArrayList<ConsumerRecord<String, ExchangeRateDTO>>();
        records.forEach(recordsList::add);

        assertThat(recordsList).hasSize(2);
        assertThat(recordsList.get(0).key()).isEqualTo("USD");
        assertThat(recordsList.get(1).key()).isEqualTo("USD");
        assertThat(recordsList.get(0).partition()).isEqualTo(recordsList.get(1).partition());
    }

    @Test
    void shouldSendMultipleCurrencies() {
        ExchangeRateDTO usdRate = ExchangeRateDTO.builder()
            .currency("USD")
            .buyRate(new BigDecimal("75.50"))
            .sellRate(new BigDecimal("76.50"))
            .build();

        ExchangeRateDTO cnyRate = ExchangeRateDTO.builder()
            .currency("CNY")
            .buyRate(new BigDecimal("10.50"))
            .sellRate(new BigDecimal("11.00"))
            .build();

        exchangeRateProducer.sendExchangeRate(usdRate);
        exchangeRateProducer.sendExchangeRate(cnyRate);

        var records = KafkaTestUtils.getRecords(consumer, Duration.ofSeconds(10));

        assertThat(records.count()).isEqualTo(2);

        var recordsList = new java.util.ArrayList<ConsumerRecord<String, ExchangeRateDTO>>();
        records.forEach(recordsList::add);

        assertThat(recordsList).hasSize(2);
    }
}
