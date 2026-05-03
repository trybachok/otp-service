package com.example.otp.domain.exception;

public final class ConflictException extends RuntimeException {

    public ConflictException(String message) {
        super(message);
    }
}