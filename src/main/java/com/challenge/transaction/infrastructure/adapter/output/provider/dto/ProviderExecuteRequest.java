package com.challenge.transaction.infrastructure.adapter.output.provider.dto;

import java.math.BigDecimal;

public record ProviderExecuteRequest(
        String accountId,
        String type,
        BigDecimal amount,
        String currency) {}
