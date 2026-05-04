package com.example.otp.application.port;

public interface OtpSender {

    void send(OtpMessage message);
}