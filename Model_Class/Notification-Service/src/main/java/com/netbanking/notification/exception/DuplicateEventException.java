package com.netbanking.notification.exception;

public class DuplicateEventException extends RuntimeException {
    public DuplicateEventException(String message) {
        super(message);
    }
}
