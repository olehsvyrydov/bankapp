package com.bank.generator.service;

import com.bank.generator.client.ExchangeClient;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.client.discovery.DiscoveryClient;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import java.util.concurrent.ThreadLocalRandom;

@Slf4j
@Service
public class RateGeneratorServiceImpl extends AbstractRateGeneratorService {

    private final ThreadLocalRandom random = ThreadLocalRandom.current();

    public RateGeneratorServiceImpl(ExchangeClient exchangeClient, DiscoveryClient discoveryClient) {
        super(exchangeClient, discoveryClient);
    }

    @Override
    public void generateAndUpdateRates() {
        if (!isTargetServiceAvailable()) {
            return;
        }
        double usdRate = random.nextDouble(70.0, 120.0);
        double cnyRate = random.nextDouble(8.0, 15.0);

        updateRate("USD",
            BigDecimal.valueOf(usdRate * 0.9).round(new MathContext(2, RoundingMode.HALF_UP)),
            BigDecimal.valueOf(usdRate * 1.01).round(new MathContext(2, RoundingMode.HALF_UP)));
        updateRate("CNY",
            BigDecimal.valueOf(cnyRate * 0.9).round(new MathContext(2, RoundingMode.HALF_UP)),
            BigDecimal.valueOf(cnyRate * 1.01).round(new MathContext(2, RoundingMode.HALF_UP)));
    }
}
