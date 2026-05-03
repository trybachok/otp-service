package com.example.otp.api.handler;

import com.example.otp.api.dto.auth.RegisterRequest;
import com.example.otp.api.dto.auth.RegisterResponse;
import com.example.otp.api.response.ErrorResponse;
import com.example.otp.api.response.HttpResponseWriter;
import com.example.otp.application.service.AuthService;
import com.example.otp.domain.exception.BadRequestException;
import com.example.otp.domain.exception.ConflictException;
import com.example.otp.domain.model.User;
import com.example.otp.infrastructure.json.JsonMapper;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

public final class AuthHandler implements HttpHandler {

    private static final Logger logger = LoggerFactory.getLogger(AuthHandler.class);

    private final AuthService authService;
    private final JsonMapper jsonMapper;
    private final HttpResponseWriter responseWriter;

    public AuthHandler(
            AuthService authService,
            JsonMapper jsonMapper,
            HttpResponseWriter responseWriter
    ) {
        this.authService = authService;
        this.jsonMapper = jsonMapper;
        this.responseWriter = responseWriter;
    }

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        long startedAt = System.currentTimeMillis();

        try {
            if ("/api/auth/register".equals(exchange.getRequestURI().getPath())) {
                handleRegister(exchange);
                return;
            }

            responseWriter.json(exchange, 404, new ErrorResponse(
                    "NOT_FOUND",
                    "Endpoint not found"
            ));
        } catch (BadRequestException exception) {
            logger.warn("Bad request on auth endpoint: {}", exception.getMessage());
            responseWriter.json(exchange, 400, new ErrorResponse(
                    "BAD_REQUEST",
                    exception.getMessage()
            ));
        } catch (ConflictException exception) {
            logger.warn("Conflict on auth endpoint: {}", exception.getMessage());
            responseWriter.json(exchange, 409, new ErrorResponse(
                    "CONFLICT",
                    exception.getMessage()
            ));
        } catch (IllegalArgumentException exception) {
            logger.warn("Invalid JSON on auth endpoint: {}", exception.getMessage());
            responseWriter.json(exchange, 400, new ErrorResponse(
                    "BAD_REQUEST",
                    "Invalid JSON request body"
            ));
        } catch (Exception exception) {
            logger.error("Unexpected error on auth endpoint", exception);
            responseWriter.json(exchange, 500, new ErrorResponse(
                    "INTERNAL_ERROR",
                    "Internal server error"
            ));
        } finally {
            long durationMs = System.currentTimeMillis() - startedAt;
            logger.info("{} {} completed in {} ms",
                    exchange.getRequestMethod(),
                    exchange.getRequestURI().getPath(),
                    durationMs
            );
        }
    }

    private void handleRegister(HttpExchange exchange) throws IOException {
        if (!"POST".equalsIgnoreCase(exchange.getRequestMethod())) {
            responseWriter.json(exchange, 405, new ErrorResponse(
                    "METHOD_NOT_ALLOWED",
                    "Method not allowed"
            ));
            return;
        }

        RegisterRequest request = jsonMapper.read(
                exchange.getRequestBody(),
                RegisterRequest.class
        );

        User user = authService.register(
                request.getLogin(),
                request.getPassword(),
                request.getRole()
        );

        RegisterResponse response = new RegisterResponse(
                user.id(),
                user.login(),
                user.role().name()
        );

        responseWriter.json(exchange, 201, response);
    }
}