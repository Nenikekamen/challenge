package com.challenge.transaction.infrastructure.adapter.input.rest.handler;

import com.challenge.transaction.domain.exception.TransactionException;
import com.challenge.transaction.infrastructure.config.MessageService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TransactionExceptionHandlerTest {

    @Mock
    private MessageService messageService;

    private TransactionExceptionHandler handler;

    @BeforeEach
    void setUp() {
        handler = new TransactionExceptionHandler(messageService);
    }

    @Test
    @DisplayName("Should return ProblemDetail with correct status, title and detail for BadRequest")
    void handleTransactionException_badRequest() {
        when(messageService.getMessage("transaction.error.title")).thenReturn("Transaction error");
        TransactionException exception = new TransactionException("Invalid parameter", HttpStatus.BAD_REQUEST.value());

        ProblemDetail problemDetail = handler.handleTransactionException(exception);

        assertNotNull(problemDetail);
        assertEquals(HttpStatus.BAD_REQUEST.value(), problemDetail.getStatus());
        assertEquals("Invalid parameter", problemDetail.getDetail());
        assertEquals("Transaction error", problemDetail.getTitle());
    }

    @Test
    @DisplayName("Should return ProblemDetail with Conflict status for 409")
    void handleTransactionException_conflict() {
        when(messageService.getMessage("transaction.error.title")).thenReturn("Transaction error");
        TransactionException exception = new TransactionException("Duplicate transaction", HttpStatus.CONFLICT.value());

        ProblemDetail problemDetail = handler.handleTransactionException(exception);

        assertNotNull(problemDetail);
        assertEquals(HttpStatus.CONFLICT.value(), problemDetail.getStatus());
        assertEquals("Duplicate transaction", problemDetail.getDetail());
        assertEquals("Transaction error", problemDetail.getTitle());
    }

    @Test
    @DisplayName("Should fallback to INTERNAL_SERVER_ERROR when status code is unresolvable")
    void handleTransactionException_unresolvableStatusCode_fallsBackTo500() {
        when(messageService.getMessage("transaction.error.title")).thenReturn("Transaction error");
        TransactionException exception = new TransactionException("Unknown status failure", 999);

        ProblemDetail problemDetail = handler.handleTransactionException(exception);

        assertNotNull(problemDetail);
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR.value(), problemDetail.getStatus());
        assertEquals("Unknown status failure", problemDetail.getDetail());
        assertEquals("Transaction error", problemDetail.getTitle());
    }
}
