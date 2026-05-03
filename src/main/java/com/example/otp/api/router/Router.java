package com.example.otp.api.router;

import com.example.otp.api.handler.AdminPingHandler;
import com.example.otp.api.handler.AuthHandler;
import com.example.otp.api.handler.HealthHandler;
import com.example.otp.api.handler.UserPingHandler;
import com.example.otp.api.middleware.AuthMiddleware;
import com.example.otp.api.middleware.RoleGuard;
import com.example.otp.api.response.HttpResponseWriter;
import com.example.otp.application.service.AuthService;
import com.example.otp.infrastructure.json.JsonMapper;
import com.sun.net.httpserver.HttpServer;

public final class Router {

    private final HttpServer server;
    private final AuthService authService;
    private final JsonMapper jsonMapper;
    private final HttpResponseWriter responseWriter;
    private final AuthMiddleware authMiddleware;
    private final RoleGuard roleGuard;

    public Router(
            HttpServer server,
            AuthService authService,
            JsonMapper jsonMapper,
            HttpResponseWriter responseWriter,
            AuthMiddleware authMiddleware,
            RoleGuard roleGuard
    ) {
        this.server = server;
        this.authService = authService;
        this.jsonMapper = jsonMapper;
        this.responseWriter = responseWriter;
        this.authMiddleware = authMiddleware;
        this.roleGuard = roleGuard;
    }

    public void registerRoutes() {
        server.createContext("/health", new HealthHandler());

        server.createContext("/api/auth", new AuthHandler(
                authService,
                jsonMapper,
                responseWriter
        ));

        server.createContext("/api/admin", new AdminPingHandler(
                authMiddleware,
                roleGuard,
                responseWriter
        ));

        server.createContext("/api/user", new UserPingHandler(
                authMiddleware,
                roleGuard,
                responseWriter
        ));
    }
}