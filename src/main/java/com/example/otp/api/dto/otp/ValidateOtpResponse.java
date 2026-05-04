package com.example.otp.api.dto.otp;

import java.util.UUID;

public final class ValidateOtpResponse {

    private final UUID otpId;
    private final String operationId;
    private final String status;
    private final boolean valid;

    public ValidateOtpResponse(UUID otpId, String operationId, String status, boolean valid) {
        this.otpId = otpId;
        this.operationId = operationId;
        this.status = status;
        this.valid = valid;
    }

    public UUID getOtpId() {
        return otpId;
    }

    public String getOperationId() {
        return operationId;
    }

    public String getStatus() {
        return status;
    }

    public boolean isValid() {
        return valid;
    }
}