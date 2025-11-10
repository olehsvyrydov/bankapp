package com.bank.transfer.client;

import com.bank.common.dto.ApiResponse;
import com.bank.common.dto.contracts.exchange.ConversionRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Slf4j
@Component
public class ExchangeClientFallback implements ExchangeClient {

    @Override
    public ApiResponse<BigDecimal> convert(ConversionRequest request) {
        log.error("Exchange service is unavailable. Cannot perform currency conversion from {} to {}",
            request.getFromCurrency(), request.getToCurrency());
        return ApiResponse.error("Exchange service is unavailable. Please try again later.");
    }
}
