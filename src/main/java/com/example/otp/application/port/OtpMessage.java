package com.example.otp.application.port;

import java.util.UUID;

public final class OtpMessage {

    private final UUID userId;
    private final String operationId;
    private final String code;
    private final String phone;
    private final String email;
    private final String telegramChatId;

    public OtpMessage(
            UUID userId,
            String operationId,
            String code,
            String phone,
            String email,
            String telegramChatId
    ) {
        this.userId = userId;
        this.operationId = operationId;
        this.code = code;
        this.phone = phone;
        this.email = email;
        this.telegramChatId = telegramChatId;
    }

    public UUID userId() {
        return userId;
    }

    public String operationId() {
        return operationId;
    }

    public String code() {
        return code;
    }

    public String phone() {
        return phone;
    }

    public String email() {
        return email;
    }

    public String telegramChatId() {
        return telegramChatId;
    }
}