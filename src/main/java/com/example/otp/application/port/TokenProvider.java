package com.example.otp.application.port;

import com.example.otp.domain.model.User;

public interface TokenProvider {

    String generate(User user);
}