package com.challenge.transaction.aplication.service;

import com.challenge.transaction.infrastructure.adapter.input.rest.dto.Status;
import com.challenge.transaction.infrastructure.adapter.input.rest.dto.TransactionResponse;
import com.challenge.transaction.infrastructure.adapter.input.rest.dto.TransactionType;
import java.util.List;

public interface RetrieveTransactionService {

    List<TransactionResponse> retrieveTransactions(String accountId, Status status, TransactionType type,
                                                   int page, int limit);
}
