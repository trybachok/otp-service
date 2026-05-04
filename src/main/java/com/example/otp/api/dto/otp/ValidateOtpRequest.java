package com.example.otp.api.dto.otp;

public final class ValidateOtpRequest {

    private String operationId;
    private String code;

    public ValidateOtpRequest() {
    }

    public String getOperationId() {
        return operationId;
    }

    public String getCode() {
        return code;
    }
}