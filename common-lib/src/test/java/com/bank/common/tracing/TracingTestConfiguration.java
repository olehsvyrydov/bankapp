package com.bank.common.tracing;

import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.context.annotation.ComponentScan;

/**
 * Test configuration for TracingIntegrationTest.
 * This configuration bootstraps a minimal Spring Boot context for testing
 * distributed tracing functionality.
 */
@SpringBootConfiguration
@EnableAutoConfiguration
@ComponentScan(basePackages = "com.bank.common.config")
public class TracingTestConfiguration {
    // Configuration class for tracing tests
    // Auto-configuration will set up all necessary beans including:
    // - Tracer
    // - ObservationRegistry
    // - KafkaTemplate (if Kafka is available)
    // - DataSource (if database is available)
}
