package com.example.otp.domain.exception;

public final class ForbiddenException extends RuntimeException {

    public ForbiddenException(String message) {
        super(message);
    }
}