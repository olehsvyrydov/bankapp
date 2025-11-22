package com.bank.common.metrics;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class CustomMetricsService {

    private final MeterRegistry meterRegistry;
    private final Counter loginSuccessCounter;
    private final Counter loginFailureCounter;
    private final Counter transferFailureCounter;
    private final Counter blockedOperationsCounter;
    private final Counter notificationFailureCounter;
    private final Counter exchangeRateUpdateSuccessCounter;
    private final Counter exchangeRateUpdateFailureCounter;

    public CustomMetricsService(MeterRegistry registry) {
        this.meterRegistry = registry;
        this.loginSuccessCounter = Counter.builder("login_attempts_total")
            .tag("status", "success")
            .description("Total successful login attempts")
            .register(registry);

        this.loginFailureCounter = Counter.builder("login_attempts_total")
            .tag("status", "failure")
            .description("Total failed login attempts")
            .register(registry);

        this.transferFailureCounter = Counter.builder("transfer_failed_total")
            .description("Total failed transfer operations")
            .register(registry);

        this.blockedOperationsCounter = Counter.builder("blocked_operations_total")
            .description("Total blocked operations by fraud detection")
            .register(registry);

        this.notificationFailureCounter = Counter.builder("notification_failed_total")
            .description("Total failed notification deliveries")
            .register(registry);

        this.exchangeRateUpdateSuccessCounter = Counter.builder("exchange_rate_update")
            .tag("status", "success")
            .description("Successful exchange rate updates")
            .register(registry);

        this.exchangeRateUpdateFailureCounter = Counter.builder("exchange_rate_update")
            .tag("status", "failure")
            .description("Failed exchange rate updates")
            .register(registry);
    }

    public void recordLoginSuccess(String username) {
        loginSuccessCounter.increment();
        log.debug("Recorded successful login for user: {}", username);
    }

    public void recordLoginFailure(String username) {
        loginFailureCounter.increment();
        log.debug("Recorded failed login for user: {}", username);
    }

    public void recordFailedTransfer(String fromAccount, String toAccount, String reason) {
        Counter.builder("transfer_failed_total")
            .tag("reason", reason)
            .description("Total failed transfer operations")
            .register(meterRegistry)
            .increment();
        log.debug("Recorded failed transfer from {} to {} with reason: {}", fromAccount, toAccount, reason);
    }

    public void recordBlockedOperation(String fromAccount, String toAccount) {
        blockedOperationsCounter.increment();
        log.debug("Recorded blocked operation from {} to {}", fromAccount, toAccount);
    }

    public void recordFailedNotification(String username) {
        notificationFailureCounter.increment();
        log.debug("Recorded failed notification for user: {}", username);
    }

    public void recordExchangeRateUpdate() {
        exchangeRateUpdateSuccessCounter.increment();
        log.debug("Recorded successful exchange rate update");
    }

    public void recordExchangeRateUpdateFailure() {
        exchangeRateUpdateFailureCounter.increment();
        log.debug("Recorded failed exchange rate update");
    }
}
