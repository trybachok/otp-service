package com.example.otp.infrastructure.security;

import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import com.example.otp.application.port.TokenProvider;
import com.example.otp.config.AppConfig;
import com.example.otp.domain.model.User;

import java.time.Instant;
import java.util.Date;

public final class JwtTokenProvider implements TokenProvider {

    private final Algorithm algorithm;
    private final long tokenTtlSeconds;

    public JwtTokenProvider(AppConfig appConfig) {
        this.algorithm = Algorithm.HMAC256(appConfig.tokenSecret());
        this.tokenTtlSeconds = appConfig.tokenTtlSeconds();
    }

    @Override
    public String generate(User user) {
        Instant now = Instant.now();
        Instant expiresAt = now.plusSeconds(tokenTtlSeconds);

        return JWT.create()
                .withSubject(user.id().toString())
                .withClaim("login", user.login())
                .withClaim("role", user.role().name())
                .withIssuedAt(Date.from(now))
                .withExpiresAt(Date.from(expiresAt))
                .sign(algorithm);
    }
}