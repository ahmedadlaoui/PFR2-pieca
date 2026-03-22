package com.pieca.backend.exceptions;

public class BusinessViolationException extends RuntimeException {
    public BusinessViolationException(String message) {
        super(message);
    }
}
