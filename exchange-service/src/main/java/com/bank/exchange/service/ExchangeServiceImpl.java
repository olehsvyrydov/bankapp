package com.bank.exchange.service;

import com.bank.common.dto.contracts.exchange.ExchangeRateDTO;
import com.bank.exchange.entity.ExchangeRate;
import com.bank.exchange.repository.ExchangeRateRepository;
import com.bank.common.exception.BusinessException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;
import java.util.stream.Collectors;

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
    public void updateRate(String currency, Double buyRate, Double sellRate) {
        ExchangeRate rate = exchangeRateRepository.findByCurrency(currency)
            .orElse(ExchangeRate.builder().currency(currency).build());

        rate.setBuyRate(buyRate);
        rate.setSellRate(sellRate);
        exchangeRateRepository.save(rate);
    }

    @Override
    public Double convert(Double amount, String fromCurrency, String toCurrency) {
        if (fromCurrency.equals(toCurrency)) {
            return amount;
        }

        // Convert to RUB first
        // When converting FROM foreign currency TO RUB: Bank BUYS foreign currency at BUY rate
        Double rubAmount = amount;
        if (!"RUB".equals(fromCurrency)) {
            ExchangeRate fromRate = exchangeRateRepository.findByCurrency(fromCurrency)
                .orElseThrow(() -> new BusinessException("Exchange rate not found for " + fromCurrency));
            // Bank buys foreign currency at buy rate
            rubAmount = amount * fromRate.getBuyRate();
        }

        // Convert from RUB to target currency
        // When converting FROM RUB TO foreign currency: Bank SELLS foreign currency at SELL rate
        if (!"RUB".equals(toCurrency)) {
            ExchangeRate toRate = exchangeRateRepository.findByCurrency(toCurrency)
                .orElseThrow(() -> new BusinessException("Exchange rate not found for " + toCurrency));
            // Bank sells foreign currency at sell rate
            return rubAmount / toRate.getSellRate();
        }

        return rubAmount;
    }
}
