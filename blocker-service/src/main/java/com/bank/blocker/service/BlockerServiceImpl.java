package com.bank.blocker.service;

import com.bank.common.dto.contracts.blocker.BlockCheckRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import java.util.concurrent.ThreadLocalRandom;

@Service
public class BlockerServiceImpl implements BlockerService {

    @Value("${blocker.suspicious-probability:0.5}")
    private double suspiciousProbability;

    @Value("${blocker.max-amount-threshold:1000}")
    private double maxAmountThreshold;

    private final ThreadLocalRandom random = ThreadLocalRandom.current();

    @Override
    public boolean checkOperation(BlockCheckRequest request) {
        if (request.getAmount() > maxAmountThreshold) {
            return true;
        }

        // Random blocking based on probability
        return random.nextDouble() < suspiciousProbability;
    }
}
