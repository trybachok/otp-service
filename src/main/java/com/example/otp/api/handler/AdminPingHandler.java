package com.example.otp.api.handler;

import com.example.otp.api.middleware.AuthMiddleware;
import com.example.otp.api.middleware.RoleGuard;
import com.example.otp.api.response.ErrorResponse;
import com.example.otp.api.response.HttpResponseWriter;
import com.example.otp.application.security.AuthenticatedUser;
import com.example.otp.domain.exception.ForbiddenException;
import com.example.otp.domain.exception.UnauthorizedException;
import com.example.otp.domain.model.Role;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Map;

public final class AdminPingHandler implements HttpHandler {

    private static final Logger logger = LoggerFactory.getLogger(AdminPingHandler.class);

    private final AuthMiddleware authMiddleware;
    private final RoleGuard roleGuard;
    private final HttpResponseWriter responseWriter;

    public AdminPingHandler(
            AuthMiddleware authMiddleware,
            RoleGuard roleGuard,
            HttpResponseWriter responseWriter
    ) {
        this.authMiddleware = authMiddleware;
        this.roleGuard = roleGuard;
        this.responseWriter = responseWriter;
    }

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        long startedAt = System.currentTimeMillis();

        try {
            if (!"/api/admin/ping".equals(exchange.getRequestURI().getPath())) {
                responseWriter.json(exchange, 404, new ErrorResponse(
                        "NOT_FOUND",
                        "Endpoint not found"
                ));
                return;
            }

            if (!"GET".equalsIgnoreCase(exchange.getRequestMethod())) {
                responseWriter.json(exchange, 405, new ErrorResponse(
                        "METHOD_NOT_ALLOWED",
                        "Method not allowed"
                ));
                return;
            }

            AuthenticatedUser user = authMiddleware.authenticate(exchange);
            roleGuard.requireRole(user, Role.ADMIN);

            responseWriter.json(exchange, 200, Map.of(
                    "status", "OK",
                    "message", "Admin access granted",
                    "userId", user.userId().toString(),
                    "role", user.role().name()
            ));

        } catch (UnauthorizedException exception) {
            logger.warn("Unauthorized admin request: {}", exception.getMessage());
            responseWriter.json(exchange, 401, new ErrorResponse(
                    "UNAUTHORIZED",
                    exception.getMessage()
            ));

        } catch (ForbiddenException exception) {
            logger.warn("Forbidden admin request: {}", exception.getMessage());
            responseWriter.json(exchange, 403, new ErrorResponse(
                    "FORBIDDEN",
                    exception.getMessage()
            ));

        } catch (Exception exception) {
            logger.error("Unexpected error on admin endpoint", exception);
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
}