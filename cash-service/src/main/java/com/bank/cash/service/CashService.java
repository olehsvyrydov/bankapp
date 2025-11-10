package com.bank.cash.service;

import com.bank.common.dto.contracts.cash.CashOperationRequest;
import com.bank.common.dto.contracts.cash.CashOperationResponse;

public interface CashService {
    CashOperationResponse processOperation(CashOperationRequest request, String username);
}
