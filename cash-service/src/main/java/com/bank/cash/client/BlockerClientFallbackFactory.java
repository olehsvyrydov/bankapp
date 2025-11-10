package com.bank.cash.client;

import com.bank.common.dto.contracts.blocker.BlockCheckResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.openfeign.FallbackFactory;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.Map;

@Slf4j
@Component
public class BlockerClientFallbackFactory implements FallbackFactory<BlockerClient> {

    @Override
    public BlockerClient create(Throwable cause) {
        return request -> {
            log.error("Fallback: Blocker service is unavailable. Cause: {}", cause.getMessage());
            return new BlockCheckResponse(false, "blocked");
        };
    }
}
