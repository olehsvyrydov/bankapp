package com.bank.transfer.client;

import com.bank.common.dto.contracts.blocker.BlockCheckRequest;
import com.bank.common.dto.contracts.blocker.BlockCheckResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class BlockerClientFallback implements BlockerClient
{
    @Override
    public BlockCheckResponse checkOperation(BlockCheckRequest request)
    {
        log.warn("Blocker service is unavailable. Fallback triggered for checkOperation.");
        return BlockCheckResponse.of(false);
    }
}
