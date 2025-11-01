package com.bank.exchange.controller;

import com.bank.common.dto.contracts.exchange.ConversionRequest;
import com.bank.common.dto.contracts.exchange.ExchangeRateDTO;
import com.bank.exchange.service.ExchangeService;
import com.bank.common.dto.ApiResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/exchange")
public class ExchangeController {

    private final ExchangeService exchangeService;

    public ExchangeController(ExchangeService exchangeService) {
        this.exchangeService = exchangeService;
    }

    @GetMapping("/rates")
    public ResponseEntity<ApiResponse<List<ExchangeRateDTO>>> getRates() {
        List<ExchangeRateDTO> rates = exchangeService.getAllRates();
        return ResponseEntity.ok(ApiResponse.success(rates));
    }

    @PostMapping("/rates")
    public ResponseEntity<ApiResponse<Void>> updateRate(@RequestBody ExchangeRateDTO rate) {
        exchangeService.updateRate(rate.getCurrency(), rate.getBuyRate(), rate.getSellRate());
        return ResponseEntity.ok(ApiResponse.success(null, "Rate updated"));
    }

    @PostMapping("/convert")
    public ResponseEntity<ApiResponse<Double>> convert(@RequestBody ConversionRequest request) {
        Double result = exchangeService.convert(
            request.getAmount(),
            request.getFromCurrency(),
            request.getToCurrency());
        return ResponseEntity.ok(ApiResponse.success(result));
    }
}
