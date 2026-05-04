package com.example.otp.api.dto.otp;

public final class DeliveryDestination {

    private String phone;
    private String email;
    private String telegramChatId;

    public DeliveryDestination() {
    }

    public String getPhone() {
        return phone;
    }

    public String getEmail() {
        return email;
    }

    public String getTelegramChatId() {
        return telegramChatId;
    }
}