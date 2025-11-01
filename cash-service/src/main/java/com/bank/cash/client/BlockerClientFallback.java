package com.bank.cash.client;

import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

@Component
public class BlockerClientFallback implements BlockerClient {

    @Override
    public Map<String, Object> checkOperation(Map<String, Object> request) {
        Map<String, Object> response = new HashMap<>();
        response.put("blocked", false);
        return response;
    }
}
