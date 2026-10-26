package com.challenge.transaction.aplication.service.impl;

import com.challenge.transaction.domain.exception.TransactionException;
import com.challenge.transaction.domain.model.Transaction;
import com.challenge.transaction.infrastructure.adapter.input.rest.dto.Status;
import com.challenge.transaction.infrastructure.adapter.input.rest.dto.TransactionResponse;
import com.challenge.transaction.infrastructure.adapter.input.rest.dto.TransactionType;
import com.challenge.transaction.infrastructure.adapter.input.rest.mapper.TransactionMapper;
import com.challenge.transaction.infrastructure.adapter.output.database.entity.TransactionEntity;
import com.challenge.transaction.infrastructure.adapter.output.database.repository.TransactionRepository;
import com.challenge.transaction.infrastructure.config.MessageService;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RetrieveTransactionServiceImplTest {

    @Mock
    private TransactionRepository transactionRepository;

    @Mock
    private TransactionMapper transactionMapper;

    @Mock
    private MessageService messageService;

    private RetrieveTransactionServiceImpl retrieveTransactionService;

    @BeforeEach
    void setUp() {
        retrieveTransactionService = new RetrieveTransactionServiceImpl(
                transactionRepository,
                transactionMapper,
                messageService
        );
    }

    @Test
    @DisplayName("Should retrieve transactions with filters successfully")
    void shouldRetrieveTransactionsWithFiltersSuccessfully() {
        TransactionEntity entity = new TransactionEntity();
        Transaction domain = new Transaction(
                "id-1", "acc-2", com.challenge.transaction.domain.model.TransactionType.CREDIT,
                new BigDecimal("100.00"), "USD", "Test", com.challenge.transaction.domain.model.Status.APPROVED,
                null, null, null, null, null, LocalDateTime.now()
        );
        TransactionResponse responseDto = new TransactionResponse(
                "id-1", "acc-2", TransactionType.CREDIT,
                new BigDecimal("100.00"), "USD", "Test", Status.APPROVED,
                null, null, null, null, null, LocalDateTime.now()
        );

        Page<TransactionEntity> page = new PageImpl<>(List.of(entity));

        when(transactionRepository.findAll(any(Specification.class), any(Pageable.class))).thenReturn(page);
        when(transactionMapper.toDomain(entity)).thenReturn(domain);
        when(transactionMapper.toResponse(domain)).thenReturn(responseDto);

        List<TransactionResponse> results = retrieveTransactionService.retrieveTransactions(
                "acc-2", Status.APPROVED, TransactionType.CREDIT, 0, 20
        );

        assertNotNull(results);
        assertEquals(1, results.size());

        ArgumentCaptor<Pageable> pageableCaptor = ArgumentCaptor.forClass(Pageable.class);
        verify(transactionRepository).findAll(any(Specification.class), pageableCaptor.capture());
        assertEquals(0, pageableCaptor.getValue().getPageNumber());
        assertEquals(20, pageableCaptor.getValue().getPageSize());
    }

    @Test
    @DisplayName("Should retrieve transactions without filters successfully")
    void shouldRetrieveTransactionsWithoutFiltersSuccessfully() {
        TransactionEntity entity = new TransactionEntity();
        Transaction domain = new Transaction(
                "id-1", "acc-2", com.challenge.transaction.domain.model.TransactionType.CREDIT,
                new BigDecimal("100.00"), "USD", "Test", com.challenge.transaction.domain.model.Status.APPROVED,
                null, null, null, null, null, LocalDateTime.now()
        );
        TransactionResponse responseDto = new TransactionResponse(
                "id-1", "acc-2", TransactionType.CREDIT,
                new BigDecimal("100.00"), "USD", "Test", Status.APPROVED,
                null, null, null, null, null, LocalDateTime.now()
        );

        Page<TransactionEntity> page = new PageImpl<>(List.of(entity));

        when(transactionRepository.findAll(any(Specification.class), any(Pageable.class))).thenReturn(page);
        when(transactionMapper.toDomain(entity)).thenReturn(domain);
        when(transactionMapper.toResponse(domain)).thenReturn(responseDto);

        List<TransactionResponse> results = retrieveTransactionService.retrieveTransactions(
                null, null, null, 0, 10
        );

        assertNotNull(results);
        assertEquals(1, results.size());
    }

    @Test
    @DisplayName("Should retrieve transactions when accountId is blank")
    void shouldRetrieveTransactionsWhenAccountIdIsBlank() {
        TransactionEntity entity = new TransactionEntity();
        Page<TransactionEntity> page = new PageImpl<>(List.of(entity));

        when(transactionRepository.findAll(any(Specification.class), any(Pageable.class))).thenReturn(page);

        List<TransactionResponse> results = retrieveTransactionService.retrieveTransactions(
                "   ", null, null, 0, 10
        );

        assertNotNull(results);
    }

    @Test
    @DisplayName("Should throw exception when page is negative")
    void shouldThrowExceptionWhenPageIsNegative() {
        when(messageService.getMessage("pagination.page.negative")).thenReturn("The page parameter cannot be negative");

        TransactionException exception = assertThrows(TransactionException.class, () ->
                retrieveTransactionService.retrieveTransactions(null, null, null, -1, 10)
        );
        assertEquals(HttpStatus.BAD_REQUEST.value(), exception.getStatusCode());
        assertEquals("The page parameter cannot be negative", exception.getMessage());
    }

    @Test
    @DisplayName("Should throw exception when limit is invalid (0 or > 100)")
    void shouldThrowExceptionWhenLimitIsInvalid() {
        when(messageService.getMessage("pagination.limit.invalid")).thenReturn("The limit parameter must be between 1 and 100");

        TransactionException exception1 = assertThrows(TransactionException.class, () ->
                retrieveTransactionService.retrieveTransactions(null, null, null, 0, 0)
        );
        assertEquals(HttpStatus.BAD_REQUEST.value(), exception1.getStatusCode());
        assertEquals("The limit parameter must be between 1 and 100", exception1.getMessage());

        TransactionException exception2 = assertThrows(TransactionException.class, () ->
                retrieveTransactionService.retrieveTransactions(null, null, null, 0, 101)
        );
        assertEquals(HttpStatus.BAD_REQUEST.value(), exception2.getStatusCode());
        assertEquals("The limit parameter must be between 1 and 100", exception2.getMessage());
    }
}
