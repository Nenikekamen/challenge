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
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SupportedCurrencyValidatorTest {

    @Mock
    private MessageService messageService;

    private SupportedCurrencyValidator validator;

    @BeforeEach
    void setUp() {
        validator = new SupportedCurrencyValidator(messageService);
    }

    @Test
    @DisplayName("Should pass validation when currency is MXN uppercase")
    void validate_currencyMxnUppercase_passes() {
        TransactionRequest request = new TransactionRequest(
                "acc-1",
                TransactionType.CREDIT,
                new BigDecimal("100.00"),
                "MXN",
                "Deposit"
        );

        assertDoesNotThrow(() -> validator.validate(request));
    }

    @Test
    @DisplayName("Should pass validation when currency is mxn lowercase with whitespace")
    void validate_currencyMxnMixedCaseWithWhitespace_passes() {
        TransactionRequest request = new TransactionRequest(
                "acc-1",
                TransactionType.CREDIT,
                new BigDecimal("100.00"),
                "  mxn  ",
                "Deposit"
        );

        assertDoesNotThrow(() -> validator.validate(request));
    }

    @Test
    @DisplayName("Should throw TransactionException when currency is unsupported (e.g. USD)")
    void validate_currencyUnsupported_throwsException() {
        when(messageService.getMessage(eq("transaction.validator.currency.unsupported"), eq("USD"), eq("MXN")))
                .thenReturn("The currency USD is not supported, only MXN is accepted");

        TransactionRequest request = new TransactionRequest(
                "acc-1",
                TransactionType.CREDIT,
                new BigDecimal("100.00"),
                "USD",
                "Deposit"
        );

        TransactionException exception = assertThrows(TransactionException.class, () -> validator.validate(request));
        assertEquals(HttpStatus.BAD_REQUEST.value(), exception.getStatusCode());
        assertEquals("The currency USD is not supported, only MXN is accepted", exception.getMessage());
    }

    @Test
    @DisplayName("Should throw TransactionException when currency is unsupported (e.g. EUR)")
    void validate_currencyUnsupportedEur_throwsException() {
        when(messageService.getMessage(eq("transaction.validator.currency.unsupported"), eq("EUR"), eq("MXN")))
                .thenReturn("The currency EUR is not supported, only MXN is accepted");

        TransactionRequest request = new TransactionRequest(
                "acc-1",
                TransactionType.CREDIT,
                new BigDecimal("100.00"),
                "EUR",
                "Deposit"
        );

        TransactionException exception = assertThrows(TransactionException.class, () -> validator.validate(request));
        assertEquals(HttpStatus.BAD_REQUEST.value(), exception.getStatusCode());
        assertEquals("The currency EUR is not supported, only MXN is accepted", exception.getMessage());
    }
}
