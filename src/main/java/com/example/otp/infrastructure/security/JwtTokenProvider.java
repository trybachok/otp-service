package com.example.otp.infrastructure.security;

import com.auth0.jwt.JWT;
import com.auth0.jwt.JWTVerifier;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.exceptions.JWTVerificationException;
import com.auth0.jwt.interfaces.DecodedJWT;
import com.example.otp.application.port.TokenProvider;
import com.example.otp.application.security.AuthenticatedUser;
import com.example.otp.config.AppConfig;
import com.example.otp.domain.exception.UnauthorizedException;
import com.example.otp.domain.model.Role;
import com.example.otp.domain.model.User;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;
import java.util.Date;
import java.util.UUID;

public final class JwtTokenProvider implements TokenProvider {

    private static final Logger logger = LoggerFactory.getLogger(JwtTokenProvider.class);

    private final Algorithm algorithm;
    private final JWTVerifier verifier;
    private final long tokenTtlSeconds;

    public JwtTokenProvider(AppConfig appConfig) {
        this.algorithm = Algorithm.HMAC256(appConfig.tokenSecret());
        this.verifier = JWT.require(algorithm).build();
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

    @Override
    public AuthenticatedUser verify(String token) {
        try {
            DecodedJWT decodedJWT = verifier.verify(token);

            UUID userId = UUID.fromString(decodedJWT.getSubject());
            String login = decodedJWT.getClaim("login").asString();
            Role role = Role.valueOf(decodedJWT.getClaim("role").asString());

            return new AuthenticatedUser(userId, login, role);
        } catch (JWTVerificationException | IllegalArgumentException exception) {
            logger.warn("JWT verification failed: {}", exception.getMessage());
            throw new UnauthorizedException("Invalid or expired token");
        }
    }
}