package com.bank.exchange.service;

import com.bank.common.dto.contracts.exchange.ExchangeRateDTO;

import java.util.List;

public interface ExchangeService {
    List<ExchangeRateDTO> getAllRates();
    void updateRate(String currency, Double buyRate, Double sellRate);
    Double convert(Double amount, String fromCurrency, String toCurrency);
}
