package com.challenge.transaction.aplication.service;

import com.challenge.transaction.infrastructure.adapter.input.rest.dto.TransactionRequest;
import com.challenge.transaction.infrastructure.adapter.input.rest.dto.TransactionResponse;

public interface AddTransactionService {

    TransactionResponse addTransaction(TransactionRequest request);
}
