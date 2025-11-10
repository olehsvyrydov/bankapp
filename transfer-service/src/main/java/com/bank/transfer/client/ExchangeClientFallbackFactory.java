package com.bank.transfer.client;

import com.bank.common.dto.ApiResponse;
import com.bank.common.dto.contracts.exchange.ConversionRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.openfeign.FallbackFactory;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Slf4j
@Component
public class ExchangeClientFallbackFactory implements FallbackFactory<ExchangeClient> {

    @Override
    public ExchangeClient create(Throwable cause) {
        return new ExchangeClient() {
            @Override
            public ApiResponse<BigDecimal> convert(ConversionRequest request) {
                log.error("Fallback: Failed to convert currency from {} to {}. Cause: {}",
                    request.getFromCurrency(), request.getToCurrency(), cause.getMessage());
                return ApiResponse.error("Exchange service unavailable");
            }
        };
    }
}

