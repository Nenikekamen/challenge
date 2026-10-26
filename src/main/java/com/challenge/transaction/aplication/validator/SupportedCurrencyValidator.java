package com.challenge.transaction.aplication.validator;

import com.challenge.transaction.domain.exception.TransactionException;
import com.challenge.transaction.infrastructure.adapter.input.rest.dto.TransactionRequest;
import com.challenge.transaction.infrastructure.config.MessageService;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

@Component
@Order(3)
public class SupportedCurrencyValidator implements TransactionValidator {

    private static final String SUPPORTED_CURRENCY = "MXN";
    private final MessageService messageService;

    public SupportedCurrencyValidator(MessageService messageService) {
        this.messageService = messageService;
    }

    @Override
    public void validate(TransactionRequest request) {
        String currency = request.currency();
        if (currency == null || !SUPPORTED_CURRENCY.equalsIgnoreCase(currency.trim())) {
            throw new TransactionException(
                    messageService.getMessage("transaction.validator.currency.unsupported", currency, SUPPORTED_CURRENCY),
                    HttpStatus.BAD_REQUEST.value());
        }
    }
}
