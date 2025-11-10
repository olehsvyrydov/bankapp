package com.bank.generator.scheduler;

import com.bank.generator.service.RateGeneratorService;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class ExchangeRateScheduler {

    private final RateGeneratorService rateGeneratorService;

    public ExchangeRateScheduler(RateGeneratorService rateGeneratorService) {
        this.rateGeneratorService = rateGeneratorService;
    }

    @Scheduled(fixedRate = 1000) // Every second
    public void generateRates() {
        rateGeneratorService.generateAndUpdateRates();
    }
}
