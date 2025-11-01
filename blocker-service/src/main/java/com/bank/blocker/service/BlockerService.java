package com.bank.blocker.service;

import com.bank.common.dto.contracts.blocker.BlockCheckRequest;

public interface BlockerService {
    boolean checkOperation(BlockCheckRequest request);
}
