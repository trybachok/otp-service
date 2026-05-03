package com.example.otp.application.port;

import com.example.otp.application.security.AuthenticatedUser;
import com.example.otp.domain.model.User;

public interface TokenProvider {

    String generate(User user);

    AuthenticatedUser verify(String token);
}