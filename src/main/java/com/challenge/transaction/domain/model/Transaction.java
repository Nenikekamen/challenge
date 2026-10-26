package com.challenge.transaction.domain.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record Transaction(
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
