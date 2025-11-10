package com.bank.exchange.service;

import com.bank.common.dto.contracts.exchange.ExchangeRateDTO;

import java.math.BigDecimal;
import java.util.List;

public interface ExchangeService {
    List<ExchangeRateDTO> getAllRates();
    void updateRate(String currency, BigDecimal buyRate, BigDecimal sellRate);
    BigDecimal convert(BigDecimal amount, String fromCurrency, String toCurrency);
}
