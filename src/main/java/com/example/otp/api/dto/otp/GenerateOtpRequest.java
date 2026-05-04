package com.example.otp.api.dto.otp;

public final class GenerateOtpRequest {

    private String operationId;
    private String description;

    public GenerateOtpRequest() {
    }

    public String getOperationId() {
        return operationId;
    }

    public String getDescription() {
        return description;
    }
}