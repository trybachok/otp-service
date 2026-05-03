package com.example.otp.infrastructure.security;

import com.example.otp.application.security.AuthenticatedUser;
import com.example.otp.config.AppConfig;
import com.example.otp.domain.exception.UnauthorizedException;
import com.example.otp.domain.model.Role;
import com.example.otp.domain.model.User;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class JwtTokenProviderTest {

    @Test
    void generateAndVerifyReturnsAuthenticatedUser() {
        JwtTokenProvider tokenProvider = new JwtTokenProvider(AppConfig.load());

        User user = new User(
                UUID.randomUUID(),
                "user1",
                "password-hash",
                Role.USER,
                Instant.now()
        );

        String token = tokenProvider.generate(user);

        AuthenticatedUser authenticatedUser = tokenProvider.verify(token);

        assertEquals(user.id(), authenticatedUser.userId());
        assertEquals(user.login(), authenticatedUser.login());
        assertEquals(user.role(), authenticatedUser.role());
    }

    @Test
    void verifyRejectsInvalidToken() {
        JwtTokenProvider tokenProvider = new JwtTokenProvider(AppConfig.load());

        UnauthorizedException exception = assertThrows(
                UnauthorizedException.class,
                () -> tokenProvider.verify("invalid-token")
        );

        assertEquals("Invalid or expired token", exception.getMessage());
    }

    @Test
    void verifyRejectsTokenSignedWithAnotherSecret() {
        JwtTokenProvider tokenProvider = new JwtTokenProvider(AppConfig.load());

        String fakeToken = com.auth0.jwt.JWT.create()
                .withSubject(UUID.randomUUID().toString())
                .withClaim("login", "user1")
                .withClaim("role", "USER")
                .sign(com.auth0.jwt.algorithms.Algorithm.HMAC256("another-secret"));

        UnauthorizedException exception = assertThrows(
                UnauthorizedException.class,
                () -> tokenProvider.verify(fakeToken)
        );

        assertEquals("Invalid or expired token", exception.getMessage());
    }
}