package com.example.otp.api.handler;

import com.example.otp.api.dto.otp.GenerateOtpRequest;
import com.example.otp.api.dto.otp.GenerateOtpResponse;
import com.example.otp.api.dto.otp.ValidateOtpRequest;
import com.example.otp.api.dto.otp.ValidateOtpResponse;
import com.example.otp.api.middleware.AuthMiddleware;
import com.example.otp.api.middleware.RoleGuard;
import com.example.otp.api.response.ErrorResponse;
import com.example.otp.api.response.HttpResponseWriter;
import com.example.otp.application.security.AuthenticatedUser;
import com.example.otp.application.service.OtpService;
import com.example.otp.domain.exception.BadRequestException;
import com.example.otp.domain.exception.ForbiddenException;
import com.example.otp.domain.exception.NotFoundException;
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

            if ("/api/user/otp/validate".equals(path)) {
                handleValidate(exchange, method, user);
                return;
            }

            responseWriter.json(exchange, 404, new ErrorResponse("NOT_FOUND", "Endpoint not found"));

        } catch (UnauthorizedException exception) {
            responseWriter.json(exchange, 401, new ErrorResponse("UNAUTHORIZED", exception.getMessage()));
        } catch (ForbiddenException exception) {
            responseWriter.json(exchange, 403, new ErrorResponse("FORBIDDEN", exception.getMessage()));
        } catch (BadRequestException exception) {
            responseWriter.json(exchange, 400, new ErrorResponse("BAD_REQUEST", exception.getMessage()));
        } catch (NotFoundException exception) {
            responseWriter.json(exchange, 404, new ErrorResponse("NOT_FOUND", exception.getMessage()));
        } catch (IllegalArgumentException exception) {
            logger.warn("Invalid user OTP request: {}", exception.getMessage(), exception);
            responseWriter.json(exchange, 400, new ErrorResponse(
                    "BAD_REQUEST",
                    exception.getMessage()
            ));
        } catch (Exception exception) {
            logger.error("Unexpected error on user OTP endpoint", exception);
            responseWriter.json(exchange, 500, new ErrorResponse("INTERNAL_ERROR", "Internal server error"));
        } finally {
            logger.info("{} {} completed in {} ms",
                    exchange.getRequestMethod(),
                    exchange.getRequestURI().getPath(),
                    System.currentTimeMillis() - startedAt
            );
        }
    }

    private void handleGenerate(HttpExchange exchange, String method, AuthenticatedUser user) throws IOException {
        if (!"POST".equalsIgnoreCase(method)) {
            responseWriter.json(exchange, 405, new ErrorResponse("METHOD_NOT_ALLOWED", "Method not allowed"));
            return;
        }

        GenerateOtpRequest request = jsonMapper.read(exchange.getRequestBody(), GenerateOtpRequest.class);

        OtpCode otpCode = otpService.generate(
                user.userId(),
                request.getOperationId(),
                request.getDescription(),
                request.getChannels(),
                request.getDestination()
        );

        responseWriter.json(exchange, 201, new GenerateOtpResponse(
                otpCode.id(),
                otpCode.operationId(),
                request.getOperationId(),
                otpCode.status().name(),
                otpCode.expiresAt()
        ));
    }

    private void handleValidate(HttpExchange exchange, String method, AuthenticatedUser user) throws IOException {
        if (!"POST".equalsIgnoreCase(method)) {
            responseWriter.json(exchange, 405, new ErrorResponse("METHOD_NOT_ALLOWED", "Method not allowed"));
            return;
        }

        ValidateOtpRequest request = jsonMapper.read(exchange.getRequestBody(), ValidateOtpRequest.class);

        OtpCode otpCode = otpService.validate(
                user.userId(),
                request.getOperationId(),
                request.getCode()
        );

        responseWriter.json(exchange, 200, new ValidateOtpResponse(
                otpCode.id(),
                request.getOperationId(),
                otpCode.status().name(),
                true
        ));
    }
}