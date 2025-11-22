package com.bank.common.metrics;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(classes = MetricsTestConfiguration.class)
@TestPropertySource(properties = {
    "management.metrics.export.simple.enabled=true"
})
class MetricsIntegrationTest {

    @Autowired
    private MeterRegistry meterRegistry;

    @Autowired
    private CustomMetricsService customMetricsService;

    @Test
    void shouldRecordLoginSuccess() {
        String username = "testuser";

        customMetricsService.recordLoginSuccess(username);

        Counter counter = meterRegistry.find("login_attempts_total")
            .tag("status", "success")
            .counter();

        assertThat(counter).isNotNull();
        assertThat(counter.count()).isGreaterThan(0);
    }

    @Test
    void shouldRecordLoginFailure() {
        String username = "testuser";

        customMetricsService.recordLoginFailure(username);

        Counter counter = meterRegistry.find("login_attempts_total")
            .tag("status", "failure")
            .counter();

        assertThat(counter).isNotNull();
        assertThat(counter.count()).isGreaterThan(0);
    }

    @Test
    void shouldRecordFailedTransfer() {
        String fromAccount = "ACC001";
        String toAccount = "ACC002";
        String reason = "insufficient_funds";

        customMetricsService.recordFailedTransfer(fromAccount, toAccount, reason);

        Counter counter = meterRegistry.find("transfer_failed_total")
            .tag("reason", reason)
            .counter();

        assertThat(counter).isNotNull();
        assertThat(counter.count()).isGreaterThan(0);
    }

    @Test
    void shouldRecordBlockedOperation() {
        String fromAccount = "ACC001";
        String toAccount = "ACC002";

        customMetricsService.recordBlockedOperation(fromAccount, toAccount);

        Counter counter = meterRegistry.find("blocked_operations_total")
            .counter();

        assertThat(counter).isNotNull();
        assertThat(counter.count()).isGreaterThan(0);
    }

    @Test
    void shouldRecordFailedNotification() {
        String username = "testuser";

        customMetricsService.recordFailedNotification(username);

        Counter counter = meterRegistry.find("notification_failed_total")
            .counter();

        assertThat(counter).isNotNull();
        assertThat(counter.count()).isGreaterThan(0);
    }

    @Test
    void shouldRecordExchangeRateUpdate() {
        customMetricsService.recordExchangeRateUpdate();

        Counter counter = meterRegistry.find("exchange_rate_update")
            .tag("status", "success")
            .counter();

        assertThat(counter).isNotNull();
        assertThat(counter.count()).isGreaterThan(0);
    }

    @Test
    void shouldRecordExchangeRateUpdateFailure() {
        customMetricsService.recordExchangeRateUpdateFailure();

        Counter counter = meterRegistry.find("exchange_rate_update")
            .tag("status", "failure")
            .counter();

        assertThat(counter).isNotNull();
        assertThat(counter.count()).isGreaterThan(0);
    }

    @Test
    void shouldIncrementMultipleTimesCorrectly() {
        String username = "testuser";

        customMetricsService.recordLoginSuccess(username);
        customMetricsService.recordLoginSuccess(username);
        customMetricsService.recordLoginSuccess(username);

        Counter counter = meterRegistry.find("login_attempts_total")
            .tag("status", "success")
            .counter();

        assertThat(counter).isNotNull();
        assertThat(counter.count()).isGreaterThanOrEqualTo(3);
    }
}
