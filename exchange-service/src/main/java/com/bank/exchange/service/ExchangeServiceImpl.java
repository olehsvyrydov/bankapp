package com.bank.exchange.service;

import com.bank.common.dto.contracts.exchange.ExchangeRateDTO;
import com.bank.exchange.entity.ExchangeRate;
import com.bank.exchange.repository.ExchangeRateRepository;
import com.bank.common.exception.BusinessException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@Transactional
public class ExchangeServiceImpl implements ExchangeService {

    private final ExchangeRateRepository exchangeRateRepository;

    public ExchangeServiceImpl(ExchangeRateRepository exchangeRateRepository) {
        this.exchangeRateRepository = exchangeRateRepository;
    }

    @Override
    public List<ExchangeRateDTO> getAllRates() {
        return exchangeRateRepository.findAll().stream()
            .map(rate -> ExchangeRateDTO.builder()
                .currency(rate.getCurrency())
                .buyRate(rate.getBuyRate())
                .sellRate(rate.getSellRate())
                .build())
            .collect(Collectors.toList());
    }

    @Override
    public void updateRate(String currency, BigDecimal buyRate, BigDecimal sellRate) {
        ExchangeRate rate = exchangeRateRepository.findByCurrency(currency)
            .orElse(ExchangeRate.builder().currency(currency).build());

        BigDecimal oldBuyRate = rate.getBuyRate();
        BigDecimal oldSellRate = rate.getSellRate();

        rate.setBuyRate(buyRate);
        rate.setSellRate(sellRate);
        ExchangeRate savedRate = exchangeRateRepository.save(rate);

        log.debug("Exchange rate updated in DB - Currency: {}, Old: [buy={}, sell={}], New: [buy={}, sell={}], DB ID: {}",
            currency, oldBuyRate, oldSellRate, savedRate.getBuyRate(), savedRate.getSellRate(), savedRate.getId());
    }

    @Override
    public BigDecimal convert(BigDecimal amount, String fromCurrency, String toCurrency) {
        if (fromCurrency.equals(toCurrency)) {
            return amount;
        }

        // Convert to RUB first
        // When converting FROM foreign currency TO RUB: Bank BUYS foreign currency at BUY rate
        BigDecimal rubAmount = amount;
        if (!"RUB".equals(fromCurrency)) {
            ExchangeRate fromRate = exchangeRateRepository.findByCurrency(fromCurrency)
                .orElseThrow(() -> new BusinessException("Exchange rate not found for " + fromCurrency));
            // Bank buys foreign currency at buy rate
            rubAmount = amount.multiply(fromRate.getBuyRate());
        }

        // Convert from RUB to target currency
        // When converting FROM RUB TO foreign currency: Bank SELLS foreign currency at SELL rate
        if (!"RUB".equals(toCurrency)) {
            ExchangeRate toRate = exchangeRateRepository.findByCurrency(toCurrency)
                .orElseThrow(() -> new BusinessException("Exchange rate not found for " + toCurrency));
            // Bank sells foreign currency at sell rate
            return rubAmount.divide(toRate.getSellRate(), new MathContext(2, RoundingMode.HALF_UP));
        }

        return rubAmount;
    }
}
