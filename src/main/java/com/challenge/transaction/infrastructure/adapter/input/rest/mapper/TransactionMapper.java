package com.challenge.transaction.infrastructure.adapter.input.rest.mapper;

import com.challenge.transaction.domain.exception.TransactionException;
import com.challenge.transaction.domain.model.Status;
import com.challenge.transaction.domain.model.Transaction;
import com.challenge.transaction.infrastructure.adapter.input.rest.dto.TransactionRequest;
import com.challenge.transaction.infrastructure.adapter.input.rest.dto.TransactionResponse;
import com.challenge.transaction.infrastructure.adapter.output.database.entity.TransactionEntity;
import com.challenge.transaction.infrastructure.adapter.output.provider.dto.ProviderExecuteRequest;
import com.challenge.transaction.infrastructure.adapter.output.provider.dto.ProviderExecuteResponse;
import com.challenge.transaction.infrastructure.config.MessageService;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.springframework.http.HttpStatus;

@Mapper(componentModel = "spring")
public interface TransactionMapper {

    ProviderExecuteRequest toProviderRequest(TransactionRequest request);

    @Mapping(target = "id", source = "id")
    @Mapping(target = "accountId", source = "request.accountId")
    @Mapping(target = "type", source = "request.type")
    @Mapping(target = "amount", source = "request.amount")
    @Mapping(target = "currency", source = "request.currency")
    @Mapping(target = "description", source = "request.description")
    @Mapping(target = "status", source = "providerResponse.status")
    @Mapping(target = "providerReference", source = "providerResponse.providerReference")
    @Mapping(target = "providerTransactionId", source = "providerResponse.transactionId")
    @Mapping(target = "balanceAfter", source = "providerResponse.balance")
    @Mapping(target = "failureCode", source = "providerResponse.code")
    @Mapping(target = "failureMessage", source = "providerResponse.message")
    @Mapping(target = "createdAt", source = "providerResponse.executedAt")
    Transaction toDomain(TransactionRequest request, ProviderExecuteResponse providerResponse, String id);

    @Mapping(target = "idempotencyKey", ignore = true)
    @Mapping(target = "updatedAt", source = "createdAt")
    TransactionEntity toEntity(Transaction transaction);

    @Mapping(target = "providerTransactionId", source = "providerTransactionId")
    Transaction toDomain(TransactionEntity entity);

    TransactionResponse toResponse(Transaction transaction);

    List<TransactionResponse> toResponses(List<Transaction> transactions);

    default Status toStatus(String providerStatus) {
        if (providerStatus == null || providerStatus.isBlank()) {
            return Status.PENDING;
        }
        try {
            return Status.valueOf(providerStatus.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new TransactionException(MessageService.get("provider.status.unknown", providerStatus), e,
                    HttpStatus.BAD_GATEWAY.value());
        }
    }

    default String toProviderStatus(Status status) {
        return status != null ? status.name() : null;
    }

    default UUID toUuid(String id) {
        return id != null ? UUID.fromString(id) : null;
    }

    default String toStringId(UUID id) {
        return id != null ? id.toString() : null;
    }

    default LocalDateTime toLocalDateTime(Instant instant) {
        Instant source = instant != null ? instant : Instant.now();
        return LocalDateTime.ofInstant(source, ZoneOffset.UTC);
    }

    default Instant toInstant(LocalDateTime localDateTime) {
        return localDateTime != null ? localDateTime.toInstant(ZoneOffset.UTC) : null;
    }
}
