package com.challenge.transaction.aplication.validator;

import com.challenge.transaction.domain.exception.TransactionException;
import com.challenge.transaction.infrastructure.adapter.input.rest.dto.TransactionRequest;
import com.challenge.transaction.infrastructure.adapter.input.rest.dto.TransactionType;
import com.challenge.transaction.infrastructure.config.MessageService;
import java.math.BigDecimal;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MaximumDebitAmountValidatorTest {

    @Mock
    private MessageService messageService;

    private MaximumDebitAmountValidator validator;

    @BeforeEach
    void setUp() {
        validator = new MaximumDebitAmountValidator(messageService);
    }

    @Test
    @DisplayName("Should pass validation when transaction type is CREDIT regardless of amount")
    void validate_creditTransaction_passes() {
        TransactionRequest request = new TransactionRequest(
                "acc-1",
                TransactionType.CREDIT,
                new BigDecimal("50000.00"),
                "MXN",
                "High value credit"
        );

        assertDoesNotThrow(() -> validator.validate(request));
    }

    @Test
    @DisplayName("Should pass validation when DEBIT amount is exactly 10000.00")
    void validate_debitAmountEqualToMaximum_passes() {
        TransactionRequest request = new TransactionRequest(
                "acc-1",
                TransactionType.DEBIT,
                new BigDecimal("10000.00"),
                "MXN",
                "Max debit"
        );

        assertDoesNotThrow(() -> validator.validate(request));
    }

    @Test
    @DisplayName("Should pass validation when DEBIT amount is less than 10000.00")
    void validate_debitAmountLessThanMaximum_passes() {
        TransactionRequest request = new TransactionRequest(
                "acc-1",
                TransactionType.DEBIT,
                new BigDecimal("500.00"),
                "MXN",
                "Normal debit"
        );

        assertDoesNotThrow(() -> validator.validate(request));
    }

    @Test
    @DisplayName("Should throw TransactionException when DEBIT amount exceeds 10000.00")
    void validate_debitAmountExceedsMaximum_throwsException() {
        when(messageService.getMessage(eq("transaction.validator.debit.max-amount"), any()))
                .thenReturn("DEBIT transactions cannot exceed 10000.00 MXN per operation");

        TransactionRequest request = new TransactionRequest(
                "acc-1",
                TransactionType.DEBIT,
                new BigDecimal("10000.01"),
                "MXN",
                "Excessive debit"
        );

        TransactionException exception = assertThrows(TransactionException.class, () -> validator.validate(request));
        assertEquals(HttpStatus.BAD_REQUEST.value(), exception.getStatusCode());
        assertEquals("DEBIT transactions cannot exceed 10000.00 MXN per operation", exception.getMessage());
    }
}
