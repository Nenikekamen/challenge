package com.challenge.transaction.aplication.validator;

import com.challenge.transaction.infrastructure.adapter.input.rest.dto.TransactionRequest;

public interface TransactionValidator {

    void validate(TransactionRequest request);
}
