package com.bank.blocker.service;

import com.bank.common.dto.contracts.blocker.BlockCheckRequest;
import com.bank.common.metrics.CustomMetricsService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.concurrent.ThreadLocalRandom;

@Service
public class BlockerServiceImpl implements BlockerService {

    private final CustomMetricsService metricsService;

    @Value("${blocker.suspicious-probability:0.5}")
    private double suspiciousProbability;

    @Value("${blocker.max-amount-threshold:1000}")
    private BigDecimal maxAmountThreshold;

    private final ThreadLocalRandom random = ThreadLocalRandom.current();

    public BlockerServiceImpl(CustomMetricsService metricsService) {
        this.metricsService = metricsService;
    }

    @Override
    public boolean checkOperation(BlockCheckRequest request) {
        if (request.getAmount().compareTo(maxAmountThreshold) > 0) {
            metricsService.recordBlockedOperation(request.getUsername(), "system");
            return true;
        }

        // Random blocking based on probability
        boolean blocked = random.nextDouble() < suspiciousProbability;
        if (blocked) {
            metricsService.recordBlockedOperation(request.getUsername(), "system");
        }
        return blocked;
    }
}
