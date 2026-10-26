package com.challenge.transaction.aplication.service.impl;

import com.challenge.transaction.aplication.port.output.TransactionProviderPort;
import com.challenge.transaction.aplication.validator.TransactionValidator;
import com.challenge.transaction.domain.exception.TransactionException;
import com.challenge.transaction.domain.model.Status;
import com.challenge.transaction.domain.model.Transaction;
import com.challenge.transaction.infrastructure.adapter.input.rest.dto.TransactionRequest;
import com.challenge.transaction.infrastructure.adapter.input.rest.dto.TransactionResponse;
import com.challenge.transaction.infrastructure.adapter.input.rest.dto.TransactionType;
import com.challenge.transaction.infrastructure.adapter.input.rest.mapper.TransactionMapper;
import com.challenge.transaction.infrastructure.adapter.output.database.entity.TransactionEntity;
import com.challenge.transaction.infrastructure.adapter.output.database.repository.TransactionRepository;
import com.challenge.transaction.infrastructure.adapter.output.provider.dto.ProviderExecuteRequest;
import com.challenge.transaction.infrastructure.adapter.output.provider.dto.ProviderExecuteResponse;
import com.challenge.transaction.infrastructure.config.MessageService;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AddTransactionServiceImplTest {

    @Mock
    private TransactionProviderPort transactionProviderPort;

    @Mock
    private TransactionRepository transactionRepository;

    @Mock
    private TransactionMapper transactionMapper;

    @Mock
    private TransactionValidator transactionValidator;

    private AddTransactionServiceImpl addTransactionService;

    @BeforeEach
    void setUp() {
        addTransactionService = new AddTransactionServiceImpl(
                transactionProviderPort,
                transactionRepository,
                transactionMapper,
                List.of(transactionValidator),
                new MessageService(null)
        );
    }

    @Test
    @DisplayName("Should throw BadRequest when request is null")
    void shouldThrowBadRequestWhenRequestIsNull() {
        TransactionException exception = assertThrows(TransactionException.class, () ->
                addTransactionService.addTransaction(null)
        );

        assertEquals(HttpStatus.BAD_REQUEST.value(), exception.getStatusCode());
        verify(transactionRepository, never()).findByIdempotencyKey(any());
        verify(transactionProviderPort, never()).execute(any());
    }

    @Test
    @DisplayName("Should throw Conflict and not call provider when idempotency key already exists")
    void shouldThrowConflictAndNotCallProviderWhenIdempotencyKeyAlreadyExists() {
        TransactionRequest request = new TransactionRequest(
                "acc-123",
                TransactionType.CREDIT,
                new BigDecimal("100.00"),
                "USD",
                "Test deposit"
        );

        when(transactionRepository.findByIdempotencyKey(any()))
                .thenReturn(Optional.of(new TransactionEntity()));

        TransactionException exception = assertThrows(TransactionException.class, () ->
                addTransactionService.addTransaction(request)
        );

        assertEquals(HttpStatus.CONFLICT.value(), exception.getStatusCode());
        verify(transactionRepository).findByIdempotencyKey(any());
        verify(transactionProviderPort, never()).execute(any());
        verify(transactionValidator, never()).validate(any());
    }

    @Test
    @DisplayName("Should execute validators and propagate validation exception without calling provider")
    void shouldPropagateValidationException() {
        TransactionRequest request = new TransactionRequest(
                "acc-123",
                TransactionType.CREDIT,
                new BigDecimal("0.50"),
                "MXN",
                "Test deposit"
        );

        when(transactionRepository.findByIdempotencyKey(any()))
                .thenReturn(Optional.empty());

        doThrow(new TransactionException("The amount must be greater than 1.00 MXN", HttpStatus.BAD_REQUEST.value()))
                .when(transactionValidator).validate(request);

        TransactionException exception = assertThrows(TransactionException.class, () ->
                addTransactionService.addTransaction(request)
        );

        assertEquals(HttpStatus.BAD_REQUEST.value(), exception.getStatusCode());
        verify(transactionValidator).validate(request);
        verify(transactionProviderPort, never()).execute(any());
        verify(transactionRepository, never()).saveAndFlush(any());
    }

    @Test
    @DisplayName("Should call provider, save entity and return response when idempotency key does not exist")
    void shouldCallProviderAndSaveWhenIdempotencyKeyDoesNotExist() {
        TransactionRequest request = new TransactionRequest(
                "acc-123",
                TransactionType.CREDIT,
                new BigDecimal("100.00"),
                "MXN",
                "Test deposit"
        );

        when(transactionRepository.findByIdempotencyKey(any()))
                .thenReturn(Optional.empty());

        ProviderExecuteRequest providerRequest = new ProviderExecuteRequest(
                "acc-123", "CREDIT", new BigDecimal("100.00"), "MXN"
        );
        when(transactionMapper.toProviderRequest(request)).thenReturn(providerRequest);

        ProviderExecuteResponse providerResponse = new ProviderExecuteResponse(
                "APPROVED", "prov-tx-123", "ref-123", new BigDecimal("500.00"), Instant.now(), null, null
        );
        when(transactionProviderPort.execute(providerRequest)).thenReturn(providerResponse);

        Transaction domainTransaction = new Transaction(
                "id-123", "acc-123", com.challenge.transaction.domain.model.TransactionType.CREDIT,
                new BigDecimal("100.00"), "MXN", "Test deposit", Status.APPROVED,
                "ref-123", "prov-tx-123", new BigDecimal("500.00"), null, null, LocalDateTime.now()
        );
        when(transactionMapper.toDomain(eq(request), eq(providerResponse), any())).thenReturn(domainTransaction);

        TransactionEntity entity = new TransactionEntity();
        when(transactionMapper.toEntity(domainTransaction)).thenReturn(entity);
        when(transactionRepository.saveAndFlush(any(TransactionEntity.class))).thenReturn(entity);
        when(transactionMapper.toDomain(any(TransactionEntity.class))).thenReturn(domainTransaction);

        TransactionResponse expectedResponse = new TransactionResponse(
                "id-123", "acc-123", TransactionType.CREDIT, new BigDecimal("100.00"), "MXN", "Test deposit",
                com.challenge.transaction.infrastructure.adapter.input.rest.dto.Status.APPROVED,
                "ref-123", "prov-tx-123", new BigDecimal("500.00"), null, null, LocalDateTime.now()
        );
        when(transactionMapper.toResponse(any(Transaction.class))).thenReturn(expectedResponse);

        TransactionResponse actualResponse = addTransactionService.addTransaction(request);

        assertNotNull(actualResponse);
        assertEquals(expectedResponse, actualResponse);
        verify(transactionValidator).validate(request);
        verify(transactionProviderPort).execute(providerRequest);
        verify(transactionRepository).saveAndFlush(entity);
    }

    @Test
    @DisplayName("Should persist REJECTED transaction when provider throws an exception")
    void shouldPersistRejectedTransactionWhenProviderThrowsException() {
        TransactionRequest request = new TransactionRequest(
                "acc-123",
                TransactionType.CREDIT,
                new BigDecimal("100.00"),
                "MXN",
                "Test deposit"
        );

        when(transactionRepository.findByIdempotencyKey(any())).thenReturn(Optional.empty());

        ProviderExecuteRequest providerRequest = new ProviderExecuteRequest(
                "acc-123", "CREDIT", new BigDecimal("100.00"), "MXN"
        );
        when(transactionMapper.toProviderRequest(request)).thenReturn(providerRequest);
        when(transactionProviderPort.execute(providerRequest)).thenThrow(new RuntimeException("Connection timeout"));

        ArgumentCaptor<ProviderExecuteResponse> fallbackResponseCaptor = ArgumentCaptor.forClass(ProviderExecuteResponse.class);
        Transaction domainTransaction = new Transaction(
                "id-123", "acc-123", com.challenge.transaction.domain.model.TransactionType.CREDIT,
                new BigDecimal("100.00"), "MXN", "Test deposit", Status.REJECTED,
                null, null, null, "PROVIDER_ERROR", "Connection timeout", LocalDateTime.now()
        );
        when(transactionMapper.toDomain(eq(request), fallbackResponseCaptor.capture(), any())).thenReturn(domainTransaction);

        TransactionEntity entity = new TransactionEntity();
        when(transactionMapper.toEntity(domainTransaction)).thenReturn(entity);
        when(transactionRepository.saveAndFlush(any(TransactionEntity.class))).thenReturn(entity);
        when(transactionMapper.toDomain(any(TransactionEntity.class))).thenReturn(domainTransaction);

        TransactionResponse expectedResponse = new TransactionResponse(
                "id-123", "acc-123", TransactionType.CREDIT, new BigDecimal("100.00"), "MXN", "Test deposit",
                com.challenge.transaction.infrastructure.adapter.input.rest.dto.Status.REJECTED,
                null, null, null, "PROVIDER_ERROR", "Connection timeout", LocalDateTime.now()
        );
        when(transactionMapper.toResponse(any(Transaction.class))).thenReturn(expectedResponse);

        TransactionResponse actualResponse = addTransactionService.addTransaction(request);

        assertNotNull(actualResponse);
        assertEquals(expectedResponse, actualResponse);
        assertEquals("REJECTED", fallbackResponseCaptor.getValue().status());
        assertEquals("PROVIDER_ERROR", fallbackResponseCaptor.getValue().code());
        assertEquals("Connection timeout", fallbackResponseCaptor.getValue().message());
        verify(transactionRepository).saveAndFlush(entity);
    }

    @Test
    @DisplayName("Should persist REJECTED transaction when provider throws TransactionException")
    void shouldPersistRejectedTransactionWhenProviderThrowsTransactionException() {
        TransactionRequest request = new TransactionRequest(
                "acc-123",
                TransactionType.CREDIT,
                new BigDecimal("100.00"),
                "MXN",
                "Test deposit"
        );

        when(transactionRepository.findByIdempotencyKey(any())).thenReturn(Optional.empty());

        ProviderExecuteRequest providerRequest = new ProviderExecuteRequest(
                "acc-123", "CREDIT", new BigDecimal("100.00"), "MXN"
        );
        when(transactionMapper.toProviderRequest(request)).thenReturn(providerRequest);
        when(transactionProviderPort.execute(providerRequest)).thenThrow(new TransactionException("Provider failure", 502));

        ArgumentCaptor<ProviderExecuteResponse> fallbackResponseCaptor = ArgumentCaptor.forClass(ProviderExecuteResponse.class);
        Transaction domainTransaction = new Transaction(
                "id-123", "acc-123", com.challenge.transaction.domain.model.TransactionType.CREDIT,
                new BigDecimal("100.00"), "MXN", "Test deposit", Status.REJECTED,
                null, null, null, "502", "Provider failure", LocalDateTime.now()
        );
        when(transactionMapper.toDomain(eq(request), fallbackResponseCaptor.capture(), any())).thenReturn(domainTransaction);

        TransactionEntity entity = new TransactionEntity();
        when(transactionMapper.toEntity(domainTransaction)).thenReturn(entity);
        when(transactionRepository.saveAndFlush(any(TransactionEntity.class))).thenReturn(entity);
        when(transactionMapper.toDomain(any(TransactionEntity.class))).thenReturn(domainTransaction);

        TransactionResponse expectedResponse = new TransactionResponse(
                "id-123", "acc-123", TransactionType.CREDIT, new BigDecimal("100.00"), "MXN", "Test deposit",
                com.challenge.transaction.infrastructure.adapter.input.rest.dto.Status.REJECTED,
                null, null, null, "502", "Provider failure", LocalDateTime.now()
        );
        when(transactionMapper.toResponse(any(Transaction.class))).thenReturn(expectedResponse);

        TransactionResponse actualResponse = addTransactionService.addTransaction(request);

        assertNotNull(actualResponse);
        assertEquals(expectedResponse, actualResponse);
        assertEquals("REJECTED", fallbackResponseCaptor.getValue().status());
        assertEquals("502", fallbackResponseCaptor.getValue().code());
        assertEquals("Provider failure", fallbackResponseCaptor.getValue().message());
        verify(transactionRepository).saveAndFlush(entity);
    }

    @Test
    @DisplayName("Should throw Conflict when saveAndFlush throws DataIntegrityViolationException on race condition")
    void shouldThrowConflictOnDataIntegrityViolationException() {
        TransactionRequest request = new TransactionRequest(
                "acc-123",
                TransactionType.CREDIT,
                new BigDecimal("100.00"),
                "MXN",
                "Test deposit"
        );

        when(transactionRepository.findByIdempotencyKey(any())).thenReturn(Optional.empty());

        ProviderExecuteRequest providerRequest = new ProviderExecuteRequest(
                "acc-123", "CREDIT", new BigDecimal("100.00"), "MXN"
        );
        when(transactionMapper.toProviderRequest(request)).thenReturn(providerRequest);

        ProviderExecuteResponse providerResponse = new ProviderExecuteResponse(
                "APPROVED", "prov-tx-123", "ref-123", new BigDecimal("500.00"), Instant.now(), null, null
        );
        when(transactionProviderPort.execute(providerRequest)).thenReturn(providerResponse);

        Transaction domainTransaction = new Transaction(
                "id-123", "acc-123", com.challenge.transaction.domain.model.TransactionType.CREDIT,
                new BigDecimal("100.00"), "MXN", "Test deposit", Status.APPROVED,
                "ref-123", "prov-tx-123", new BigDecimal("500.00"), null, null, LocalDateTime.now()
        );
        when(transactionMapper.toDomain(eq(request), eq(providerResponse), any())).thenReturn(domainTransaction);

        TransactionEntity entity = new TransactionEntity();
        when(transactionMapper.toEntity(domainTransaction)).thenReturn(entity);
        when(transactionRepository.saveAndFlush(any(TransactionEntity.class)))
                .thenThrow(new DataIntegrityViolationException("Unique constraint violation"));

        TransactionException exception = assertThrows(TransactionException.class, () ->
                addTransactionService.addTransaction(request)
        );

        assertEquals(HttpStatus.CONFLICT.value(), exception.getStatusCode());
    }
}
