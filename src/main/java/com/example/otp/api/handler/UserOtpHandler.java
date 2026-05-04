package com.example.otp.api.handler;

import com.example.otp.api.dto.otp.GenerateOtpRequest;
import com.example.otp.api.dto.otp.GenerateOtpResponse;
import com.example.otp.api.middleware.AuthMiddleware;
import com.example.otp.api.middleware.RoleGuard;
import com.example.otp.api.response.ErrorResponse;
import com.example.otp.api.response.HttpResponseWriter;
import com.example.otp.application.security.AuthenticatedUser;
import com.example.otp.application.service.OtpService;
import com.example.otp.domain.exception.BadRequestException;
import com.example.otp.domain.exception.ForbiddenException;
import com.example.otp.domain.exception.UnauthorizedException;
import com.example.otp.domain.model.OtpCode;
import com.example.otp.domain.model.Role;
import com.example.otp.infrastructure.json.JsonMapper;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

public final class UserOtpHandler implements HttpHandler {

    private static final Logger logger = LoggerFactory.getLogger(UserOtpHandler.class);

    private final AuthMiddleware authMiddleware;
    private final RoleGuard roleGuard;
    private final OtpService otpService;
    private final JsonMapper jsonMapper;
    private final HttpResponseWriter responseWriter;

    public UserOtpHandler(
            AuthMiddleware authMiddleware,
            RoleGuard roleGuard,
            OtpService otpService,
            JsonMapper jsonMapper,
            HttpResponseWriter responseWriter
    ) {
        this.authMiddleware = authMiddleware;
        this.roleGuard = roleGuard;
        this.otpService = otpService;
        this.jsonMapper = jsonMapper;
        this.responseWriter = responseWriter;
    }

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        long startedAt = System.currentTimeMillis();

        try {
            AuthenticatedUser user = authMiddleware.authenticate(exchange);
            roleGuard.requireRole(user, Role.USER);

            String path = exchange.getRequestURI().getPath();
            String method = exchange.getRequestMethod();

            if ("/api/user/otp/generate".equals(path)) {
                handleGenerate(exchange, method, user);
                return;
            }

            responseWriter.json(exchange, 404, new ErrorResponse(
                    "NOT_FOUND",
                    "Endpoint not found"
            ));

        } catch (UnauthorizedException exception) {
            logger.warn("Unauthorized user OTP request: {}", exception.getMessage());
            responseWriter.json(exchange, 401, new ErrorResponse("UNAUTHORIZED", exception.getMessage()));

        } catch (ForbiddenException exception) {
            logger.warn("Forbidden user OTP request: {}", exception.getMessage());
            responseWriter.json(exchange, 403, new ErrorResponse("FORBIDDEN", exception.getMessage()));

        } catch (BadRequestException exception) {
            logger.warn("Bad user OTP request: {}", exception.getMessage());
            responseWriter.json(exchange, 400, new ErrorResponse("BAD_REQUEST", exception.getMessage()));

        } catch (IllegalArgumentException exception) {
            logger.warn("Invalid user OTP request: {}", exception.getMessage());
            responseWriter.json(exchange, 400, new ErrorResponse("BAD_REQUEST", "Invalid request"));

        } catch (Exception exception) {
            logger.error("Unexpected error on user OTP endpoint", exception);
            responseWriter.json(exchange, 500, new ErrorResponse("INTERNAL_ERROR", "Internal server error"));

        } finally {
            long durationMs = System.currentTimeMillis() - startedAt;
            logger.info("{} {} completed in {} ms",
                    exchange.getRequestMethod(),
                    exchange.getRequestURI().getPath(),
                    durationMs
            );
        }
    }

    private void handleGenerate(
            HttpExchange exchange,
            String method,
            AuthenticatedUser user
    ) throws IOException {
        if (!"POST".equalsIgnoreCase(method)) {
            responseWriter.json(exchange, 405, new ErrorResponse(
                    "METHOD_NOT_ALLOWED",
                    "Method not allowed"
            ));
            return;
        }

        GenerateOtpRequest request = jsonMapper.read(
                exchange.getRequestBody(),
                GenerateOtpRequest.class
        );

        OtpCode otpCode = otpService.generate(
                user.userId(),
                request.getOperationId(),
                request.getDescription()
        );

        GenerateOtpResponse response = new GenerateOtpResponse(
                otpCode.id(),
                otpCode.operationId(),
                request.getOperationId(),
                otpCode.status().name(),
                otpCode.expiresAt()
        );

        responseWriter.json(exchange, 201, response);
    }
}