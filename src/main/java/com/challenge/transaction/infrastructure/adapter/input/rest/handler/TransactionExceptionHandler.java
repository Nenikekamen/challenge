package com.challenge.transaction.infrastructure.adapter.input.rest.handler;

import com.challenge.transaction.domain.exception.TransactionException;
import com.challenge.transaction.infrastructure.config.MessageService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class TransactionExceptionHandler {

    private final MessageService messageService;

    public TransactionExceptionHandler(MessageService messageService) {
        this.messageService = messageService;
    }

    @ExceptionHandler(TransactionException.class)
    public ProblemDetail handleTransactionException(TransactionException ex) {
        HttpStatus status = HttpStatus.resolve(ex.getStatusCode());
        if (status == null) {
            status = HttpStatus.INTERNAL_SERVER_ERROR;
        }
        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(status, ex.getMessage());
        problemDetail.setTitle(messageService.getMessage("transaction.error.title"));
        return problemDetail;
    }
}
