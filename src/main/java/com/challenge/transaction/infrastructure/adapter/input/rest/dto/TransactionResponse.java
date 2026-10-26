package com.challenge.transaction.infrastructure.adapter.input.rest.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record TransactionResponse(
        String id,
        String accountId,
        TransactionType type,
        BigDecimal amount,
        String currency,
        String description,
        Status status,
        String providerReference,
        String providerTransactionId,
        BigDecimal balanceAfter,
        String failureCode,
        String failureMessage,
        LocalDateTime createdAt) {}
