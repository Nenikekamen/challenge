package com.challenge.transaction.aplication.validator;

import com.challenge.transaction.domain.exception.TransactionException;
import com.challenge.transaction.infrastructure.adapter.input.rest.dto.TransactionRequest;
import com.challenge.transaction.infrastructure.config.MessageService;
import java.math.BigDecimal;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

@Component
@Order(1)
public class MinimumAmountValidator implements TransactionValidator {

    private static final BigDecimal MINIMUM_AMOUNT = BigDecimal.ONE;
    private final MessageService messageService;

    public MinimumAmountValidator(MessageService messageService) {
        this.messageService = messageService;
    }

    @Override
    public void validate(TransactionRequest request) {
        BigDecimal amount = request.amount();
        if (amount == null || amount.compareTo(MINIMUM_AMOUNT) <= 0) {
            throw new TransactionException(
                    messageService.getMessage("transaction.validator.amount.min", MINIMUM_AMOUNT.toPlainString()),
                    HttpStatus.BAD_REQUEST.value());
        }
    }
}
