package com.challenge.transaction.infrastructure.adapter.input.rest.dto;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Objects;

public record TransactionRequest(
        String accountId,
        TransactionType type,
        BigDecimal amount,
        String currency,
        String description) {

    public TransactionRequest {
        Objects.requireNonNull(accountId, "accountId");
        Objects.requireNonNull(type, "type");
        Objects.requireNonNull(amount, "amount");
        Objects.requireNonNull(currency, "currency");
        amount = amount.setScale(2, RoundingMode.UNNECESSARY);
    }
}
