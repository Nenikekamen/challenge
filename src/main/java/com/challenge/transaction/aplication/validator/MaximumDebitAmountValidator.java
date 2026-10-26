package com.challenge.transaction.aplication.validator;

import com.challenge.transaction.domain.exception.TransactionException;
import com.challenge.transaction.infrastructure.adapter.input.rest.dto.TransactionRequest;
import com.challenge.transaction.infrastructure.adapter.input.rest.dto.TransactionType;
import com.challenge.transaction.infrastructure.config.MessageService;
import java.math.BigDecimal;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

@Component
@Order(2)
public class MaximumDebitAmountValidator implements TransactionValidator {

    private static final BigDecimal MAXIMUM_DEBIT_AMOUNT = new BigDecimal("10000.00");
    private final MessageService messageService;

    public MaximumDebitAmountValidator(MessageService messageService) {
        this.messageService = messageService;
    }

    @Override
    public void validate(TransactionRequest request) {
        if (request.type() != TransactionType.DEBIT) {
            return;
        }
        BigDecimal amount = request.amount();
        if (amount != null && amount.compareTo(MAXIMUM_DEBIT_AMOUNT) > 0) {
            throw new TransactionException(
                    messageService.getMessage("transaction.validator.debit.max-amount", MAXIMUM_DEBIT_AMOUNT.toPlainString()),
                    HttpStatus.BAD_REQUEST.value());
        }
    }
}
