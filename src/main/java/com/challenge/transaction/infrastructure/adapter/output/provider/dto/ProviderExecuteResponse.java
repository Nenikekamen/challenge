package com.challenge.transaction.infrastructure.adapter.output.provider.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.math.BigDecimal;
import java.time.Instant;

@JsonIgnoreProperties(ignoreUnknown = true)
public record ProviderExecuteResponse(
        String providerReference,
        String transactionId,
        String status,
        BigDecimal balance,
        Instant executedAt,
        String code,
        String message) {}
