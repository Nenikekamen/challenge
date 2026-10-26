package com.challenge.transaction.domain.exception;

public class TransactionException extends RuntimeException {
    private final int statusCode;

    public TransactionException(String message, int statusCode) {
        super(message);
        this.statusCode = statusCode;
    }

    public TransactionException(String message, Throwable cause, int statusCode) {
        super(message, cause);
        this.statusCode = statusCode;
    }

    public int getStatusCode() {
        return statusCode;
    }
}
