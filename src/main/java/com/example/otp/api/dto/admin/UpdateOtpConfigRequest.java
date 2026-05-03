package com.example.otp.api.dto.admin;

public final class UpdateOtpConfigRequest {

    private int codeLength;
    private int ttlSeconds;

    public UpdateOtpConfigRequest() {
    }

    public int getCodeLength() {
        return codeLength;
    }

    public int getTtlSeconds() {
        return ttlSeconds;
    }
}