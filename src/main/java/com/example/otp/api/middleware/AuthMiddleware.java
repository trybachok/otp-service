package com.example.otp.api.middleware;

import com.example.otp.application.port.TokenProvider;
import com.example.otp.application.security.AuthenticatedUser;
import com.example.otp.domain.exception.UnauthorizedException;
import com.sun.net.httpserver.HttpExchange;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class AuthMiddleware {

    private static final Logger logger = LoggerFactory.getLogger(AuthMiddleware.class);

    private final TokenProvider tokenProvider;

    public AuthMiddleware(TokenProvider tokenProvider) {
        this.tokenProvider = tokenProvider;
    }

    public AuthenticatedUser authenticate(HttpExchange exchange) {
        String authorizationHeader = exchange.getRequestHeaders().getFirst("Authorization");

        if (authorizationHeader == null || authorizationHeader.isBlank()) {
            logger.warn("Authorization failed: missing Authorization header");
            throw new UnauthorizedException("Authorization header is required");
        }

        if (!authorizationHeader.startsWith("Bearer ")) {
            logger.warn("Authorization failed: invalid Authorization header format");
            throw new UnauthorizedException("Authorization header must start with Bearer");
        }

        String token = authorizationHeader.substring("Bearer ".length()).trim();

        if (token.isBlank()) {
            logger.warn("Authorization failed: empty token");
            throw new UnauthorizedException("Token is required");
        }

        AuthenticatedUser authenticatedUser = tokenProvider.verify(token);

        logger.info("Request authenticated userId={} login={} role={}",
                authenticatedUser.userId(),
                authenticatedUser.login(),
                authenticatedUser.role()
        );

        return authenticatedUser;
    }
}