package com.bank.transfer.service;

import com.bank.common.dto.contracts.transfer.TransferRequest;
import com.bank.common.dto.contracts.transfer.TransferResponse;

public interface TransferService {
    TransferResponse processTransfer(TransferRequest request, String username);
}
