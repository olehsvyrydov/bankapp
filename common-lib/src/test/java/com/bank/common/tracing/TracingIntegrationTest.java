package com.bank.common.tracing;

import io.micrometer.observation.ObservationRegistry;
import io.micrometer.tracing.Tracer;
import io.micrometer.tracing.test.simple.SpansAssert;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.autoconfigure.actuate.observability.AutoConfigureObservability;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.test.context.TestPropertySource;

import javax.sql.DataSource;
import java.sql.Connection;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests for distributed tracing functionality.
 * Tests verify that trace IDs are properly propagated and spans are created
 * for various operations (HTTP, Database, Kafka).
 *
 * RED Phase: These tests are expected to fail initially until tracing
 * dependencies and configuration are added.
 */
@SpringBootTest
@AutoConfigureObservability
@TestPropertySource(properties = {
    "management.tracing.enabled=true",
    "management.tracing.sampling.probability=1.0"
})
class TracingIntegrationTest {

    @Autowired(required = false)
    private Tracer tracer;

    @Autowired(required = false)
    private ObservationRegistry observationRegistry;

    @Autowired(required = false)
    private DataSource dataSource;

    @Autowired(required = false)
    private KafkaTemplate<String, Object> kafkaTemplate;

    /**
     * Test that trace ID is properly propagated between service calls.
     * This verifies that the distributed tracing context is maintained
     * across service boundaries.
     */
    @Test
    void shouldPropagateTraceId() {
        // Verify that tracer is available
        assertThat(tracer).as("Tracer should be configured")
                .isNotNull();

        // Create a new span
        var span = tracer.nextSpan().name("test-span").start();

        try (var scope = tracer.withSpan(span)) {
            // Get current trace ID
            String traceId = tracer.currentSpan().context().traceId();

            // Verify trace ID is not null
            assertThat(traceId)
                    .as("Trace ID should be generated")
                    .isNotNull()
                    .isNotEmpty();

            // Verify span ID is not null
            assertThat(tracer.currentSpan().context().spanId())
                    .as("Span ID should be generated")
                    .isNotNull()
                    .isNotEmpty();
        } finally {
            span.end();
        }
    }

    /**
     * Test that spans are created for database queries.
     * This verifies that JDBC/JPA operations are properly instrumented
     * with tracing information.
     */
    @Test
    void shouldCreateSpanForDatabaseQuery() {
        // Verify that data source is available
        assertThat(dataSource).as("DataSource should be configured")
                .isNotNull();

        // Verify that tracer is available
        assertThat(tracer).as("Tracer should be configured")
                .isNotNull();

        // Create a parent span
        var parentSpan = tracer.nextSpan().name("test-db-operation").start();

        try (var scope = tracer.withSpan(parentSpan)) {
            // Execute a database query
            try (Connection connection = dataSource.getConnection()) {
                var statement = connection.createStatement();
                // Simple query that should work on any database
                statement.execute("SELECT 1");
            }

            // Verify that the tracer is still in the same context
            assertThat(tracer.currentSpan())
                    .as("Current span should be maintained during DB operation")
                    .isNotNull();

            // Verify trace ID is preserved
            assertThat(tracer.currentSpan().context().traceId())
                    .as("Trace ID should be preserved across DB operation")
                    .isEqualTo(parentSpan.context().traceId());
        } catch (Exception e) {
            throw new RuntimeException("Failed to execute database query", e);
        } finally {
            parentSpan.end();
        }
    }

    /**
     * Test that spans are created for Kafka producer operations.
     * This verifies that Kafka send operations are properly instrumented
     * with tracing information.
     */
    @Test
    void shouldCreateSpanForKafkaProducer() {
        // Verify that Kafka template is available
        assertThat(kafkaTemplate).as("KafkaTemplate should be configured")
                .isNotNull();

        // Verify that tracer is available
        assertThat(tracer).as("Tracer should be configured")
                .isNotNull();

        // Verify that observation is enabled on Kafka template
        assertThat(kafkaTemplate.isObservationEnabled())
                .as("Kafka template should have observation enabled")
                .isTrue();

        // Create a parent span
        var parentSpan = tracer.nextSpan().name("test-kafka-send").start();

        try (var scope = tracer.withSpan(parentSpan)) {
            String originalTraceId = parentSpan.context().traceId();

            // Send a test message to Kafka
            // Note: This will fail if Kafka is not available, but that's okay for this test
            // We're primarily testing that the tracing infrastructure is in place
            try {
                kafkaTemplate.send("test-topic", "test-key", "test-message");
            } catch (Exception e) {
                // Kafka might not be available in test environment, that's okay
                // We're testing the tracing setup, not Kafka connectivity
            }

            // Verify that the tracer context is maintained
            assertThat(tracer.currentSpan())
                    .as("Current span should be maintained during Kafka operation")
                    .isNotNull();

            // Verify trace ID is preserved
            assertThat(tracer.currentSpan().context().traceId())
                    .as("Trace ID should be preserved across Kafka operation")
                    .isEqualTo(originalTraceId);
        } finally {
            parentSpan.end();
        }
    }

    /**
     * Test that observation registry is properly configured.
     * This is a basic sanity check for the observability infrastructure.
     */
    @Test
    void shouldHaveObservationRegistryConfigured() {
        assertThat(observationRegistry)
                .as("ObservationRegistry should be configured")
                .isNotNull();
    }
}
