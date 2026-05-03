package com.example.otp.api.handler;

import com.example.otp.api.dto.admin.AdminUserResponse;
import com.example.otp.api.dto.admin.OtpConfigResponse;
import com.example.otp.api.dto.admin.UpdateOtpConfigRequest;
import com.example.otp.api.middleware.AuthMiddleware;
import com.example.otp.api.middleware.RoleGuard;
import com.example.otp.api.response.ErrorResponse;
import com.example.otp.api.response.HttpResponseWriter;
import com.example.otp.application.security.AuthenticatedUser;
import com.example.otp.application.service.OtpConfigService;
import com.example.otp.application.service.UserService;
import com.example.otp.domain.exception.BadRequestException;
import com.example.otp.domain.exception.ForbiddenException;
import com.example.otp.domain.exception.NotFoundException;
import com.example.otp.domain.exception.UnauthorizedException;
import com.example.otp.domain.model.OtpConfig;
import com.example.otp.domain.model.Role;
import com.example.otp.domain.model.User;
import com.example.otp.infrastructure.json.JsonMapper;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public final class AdminHandler implements HttpHandler {

    private static final Logger logger = LoggerFactory.getLogger(AdminHandler.class);

    private final AuthMiddleware authMiddleware;
    private final RoleGuard roleGuard;
    private final OtpConfigService otpConfigService;
    private final UserService userService;
    private final JsonMapper jsonMapper;
    private final HttpResponseWriter responseWriter;

    public AdminHandler(
            AuthMiddleware authMiddleware,
            RoleGuard roleGuard,
            OtpConfigService otpConfigService,
            UserService userService,
            JsonMapper jsonMapper,
            HttpResponseWriter responseWriter
    ) {
        this.authMiddleware = authMiddleware;
        this.roleGuard = roleGuard;
        this.otpConfigService = otpConfigService;
        this.userService = userService;
        this.jsonMapper = jsonMapper;
        this.responseWriter = responseWriter;
    }

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        long startedAt = System.currentTimeMillis();

        try {
            AuthenticatedUser user = authMiddleware.authenticate(exchange);
            roleGuard.requireRole(user, Role.ADMIN);

            String path = exchange.getRequestURI().getPath();
            String method = exchange.getRequestMethod();

            if ("/api/admin/ping".equals(path)) {
                handlePing(exchange, method, user);
                return;
            }

            if ("/api/admin/otp-config".equals(path)) {
                handleOtpConfig(exchange, method);
                return;
            }

            if ("/api/admin/users".equals(path)) {
                handleUsers(exchange, method);
                return;
            }

            if (path.startsWith("/api/admin/users/")) {
                handleUserById(exchange, method, path);
                return;
            }

            responseWriter.json(exchange, 404, new ErrorResponse(
                    "NOT_FOUND",
                    "Endpoint not found"
            ));

        } catch (UnauthorizedException exception) {
            logger.warn("Unauthorized admin request: {}", exception.getMessage());
            responseWriter.json(exchange, 401, new ErrorResponse("UNAUTHORIZED", exception.getMessage()));

        } catch (ForbiddenException exception) {
            logger.warn("Forbidden admin request: {}", exception.getMessage());
            responseWriter.json(exchange, 403, new ErrorResponse("FORBIDDEN", exception.getMessage()));

        } catch (BadRequestException exception) {
            logger.warn("Bad admin request: {}", exception.getMessage());
            responseWriter.json(exchange, 400, new ErrorResponse("BAD_REQUEST", exception.getMessage()));

        } catch (NotFoundException exception) {
            logger.warn("Admin resource not found: {}", exception.getMessage());
            responseWriter.json(exchange, 404, new ErrorResponse("NOT_FOUND", exception.getMessage()));

        } catch (IllegalArgumentException exception) {
            logger.warn("Invalid admin request: {}", exception.getMessage());
            responseWriter.json(exchange, 400, new ErrorResponse("BAD_REQUEST", "Invalid request"));

        } catch (Exception exception) {
            logger.error("Unexpected error on admin endpoint", exception);
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

    private void handlePing(HttpExchange exchange, String method, AuthenticatedUser user) throws IOException {
        if (!"GET".equalsIgnoreCase(method)) {
            methodNotAllowed(exchange);
            return;
        }

        responseWriter.json(exchange, 200, Map.of(
                "status", "OK",
                "message", "Admin access granted",
                "userId", user.userId().toString(),
                "role", user.role().name()
        ));
    }

    private void handleOtpConfig(HttpExchange exchange, String method) throws IOException {
        if (!"PUT".equalsIgnoreCase(method)) {
            methodNotAllowed(exchange);
            return;
        }

        UpdateOtpConfigRequest request = jsonMapper.read(
                exchange.getRequestBody(),
                UpdateOtpConfigRequest.class
        );

        OtpConfig updatedConfig = otpConfigService.updateConfig(
                request.getCodeLength(),
                request.getTtlSeconds()
        );

        responseWriter.json(exchange, 200, new OtpConfigResponse(
                updatedConfig.codeLength(),
                updatedConfig.ttlSeconds(),
                updatedConfig.updatedAt()
        ));
    }

    private void handleUsers(HttpExchange exchange, String method) throws IOException {
        if (!"GET".equalsIgnoreCase(method)) {
            methodNotAllowed(exchange);
            return;
        }

        List<AdminUserResponse> users = userService.getAllNonAdminUsers()
                .stream()
                .map(this::toAdminUserResponse)
                .toList();

        responseWriter.json(exchange, 200, users);
    }

    private void handleUserById(HttpExchange exchange, String method, String path) throws IOException {
        if (!"DELETE".equalsIgnoreCase(method)) {
            methodNotAllowed(exchange);
            return;
        }

        String rawUserId = path.substring("/api/admin/users/".length());

        if (rawUserId.isBlank()) {
            throw new BadRequestException("User id is required");
        }

        UUID userId = UUID.fromString(rawUserId);

        userService.deleteUser(userId);

        responseWriter.json(exchange, 200, Map.of(
                "status", "OK",
                "message", "User deleted"
        ));
    }

    private AdminUserResponse toAdminUserResponse(User user) {
        return new AdminUserResponse(
                user.id(),
                user.login(),
                user.role().name(),
                user.createdAt()
        );
    }

    private void methodNotAllowed(HttpExchange exchange) throws IOException {
        responseWriter.json(exchange, 405, new ErrorResponse(
                "METHOD_NOT_ALLOWED",
                "Method not allowed"
        ));
    }
}