package com.example.otp.application.port;

public interface OtpSender {

    com.example.otp.domain.model.OtpChannel channel();

    void send(OtpMessage message);
}