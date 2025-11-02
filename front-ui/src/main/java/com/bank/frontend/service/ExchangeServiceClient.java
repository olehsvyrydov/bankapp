package com.bank.frontend.service;

import com.bank.common.dto.ApiResponse;
import com.bank.common.dto.contracts.exchange.ExchangeRateDTO;
import com.bank.frontend.client.ExchangeClient;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ExchangeServiceClient {

    private final ExchangeClient exchangeClient;

    public ExchangeServiceClient(ExchangeClient exchangeClient) {
        this.exchangeClient = exchangeClient;
    }

    public List<ExchangeRateDTO> getExchangeRates() {
        ApiResponse<List<ExchangeRateDTO>> response = exchangeClient.getExchangeRates();
        if (!response.isSuccess()) {
            throw new RuntimeException("Failed to fetch exchange rates: " + response.getMessage());
        }
        return response.getData();
    }
}
