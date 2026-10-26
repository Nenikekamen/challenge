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
class MinimumAmountValidatorTest {

    @Mock
    private MessageService messageService;

    private MinimumAmountValidator validator;

    @BeforeEach
    void setUp() {
        validator = new MinimumAmountValidator(messageService);
    }

    @Test
    @DisplayName("Should pass validation when amount is strictly greater than 1.00")
    void validate_amountGreaterThanMinimum_passes() {
        TransactionRequest request = new TransactionRequest(
                "acc-1",
                TransactionType.CREDIT,
                new BigDecimal("1.01"),
                "MXN",
                "Valid deposit"
        );

        assertDoesNotThrow(() -> validator.validate(request));
    }

    @Test
    @DisplayName("Should throw TransactionException when amount is equal to 1.00")
    void validate_amountEqualToMinimum_throwsException() {
        when(messageService.getMessage(eq("transaction.validator.amount.min"), any()))
                .thenReturn("The amount must be greater than 1.00 MXN");

        TransactionRequest request = new TransactionRequest(
                "acc-1",
                TransactionType.CREDIT,
                new BigDecimal("1.00"),
                "MXN",
                "Deposit"
        );

        TransactionException exception = assertThrows(TransactionException.class, () -> validator.validate(request));
        assertEquals(HttpStatus.BAD_REQUEST.value(), exception.getStatusCode());
        assertEquals("The amount must be greater than 1.00 MXN", exception.getMessage());
    }

    @Test
    @DisplayName("Should throw TransactionException when amount is less than 1.00")
    void validate_amountLessThanMinimum_throwsException() {
        when(messageService.getMessage(eq("transaction.validator.amount.min"), any()))
                .thenReturn("The amount must be greater than 1.00 MXN");

        TransactionRequest request = new TransactionRequest(
                "acc-1",
                TransactionType.CREDIT,
                new BigDecimal("0.50"),
                "MXN",
                "Deposit"
        );

        TransactionException exception = assertThrows(TransactionException.class, () -> validator.validate(request));
        assertEquals(HttpStatus.BAD_REQUEST.value(), exception.getStatusCode());
        assertEquals("The amount must be greater than 1.00 MXN", exception.getMessage());
    }

    @Test
    @DisplayName("Should throw TransactionException when amount is negative")
    void validate_amountNegative_throwsException() {
        when(messageService.getMessage(eq("transaction.validator.amount.min"), any()))
                .thenReturn("The amount must be greater than 1.00 MXN");

        TransactionRequest request = new TransactionRequest(
                "acc-1",
                TransactionType.CREDIT,
                new BigDecimal("-5.00"),
                "MXN",
                "Negative deposit"
        );

        TransactionException exception = assertThrows(TransactionException.class, () -> validator.validate(request));
        assertEquals(HttpStatus.BAD_REQUEST.value(), exception.getStatusCode());
        assertEquals("The amount must be greater than 1.00 MXN", exception.getMessage());
    }
}
