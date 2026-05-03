package com.example.otp.domain.exception;

public final class BadRequestException extends RuntimeException {

    public BadRequestException(String message) {
        super(message);
    }
}