package com.example.otp.domain.exception;

public final class NotFoundException extends RuntimeException {

    public NotFoundException(String message) {
        super(message);
    }
}