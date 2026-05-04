package com.example.otp.api.router;

import com.example.otp.api.handler.AdminHandler;
import com.example.otp.api.handler.AdminPingHandler;
import com.example.otp.api.handler.AuthHandler;
import com.example.otp.api.handler.HealthHandler;
import com.example.otp.api.handler.UserOtpHandler;
import com.example.otp.api.handler.UserPingHandler;
import com.example.otp.api.middleware.AuthMiddleware;
import com.example.otp.api.middleware.RoleGuard;
import com.example.otp.api.response.HttpResponseWriter;
import com.example.otp.application.service.AuthService;
import com.example.otp.application.service.OtpConfigService;
import com.example.otp.application.service.OtpService;
import com.example.otp.application.service.UserService;
import com.example.otp.infrastructure.json.JsonMapper;
import com.sun.net.httpserver.HttpServer;

public final class Router {

    private final HttpServer server;
    private final AuthService authService;
    private final JsonMapper jsonMapper;
    private final HttpResponseWriter responseWriter;
    private final AuthMiddleware authMiddleware;
    private final RoleGuard roleGuard;
    private final OtpConfigService otpConfigService;
    private final UserService userService;
    private final OtpService otpService;

    public Router(
            HttpServer server,
            AuthService authService,
            JsonMapper jsonMapper,
            HttpResponseWriter responseWriter,
            AuthMiddleware authMiddleware,
            RoleGuard roleGuard
    ) {
        this(
                server,
                authService,
                jsonMapper,
                responseWriter,
                authMiddleware,
                roleGuard,
                null,
                null,
                null
        );
    }

    public Router(
            HttpServer server,
            AuthService authService,
            JsonMapper jsonMapper,
            HttpResponseWriter responseWriter,
            AuthMiddleware authMiddleware,
            RoleGuard roleGuard,
            OtpConfigService otpConfigService,
            UserService userService
    ) {
        this(
                server,
                authService,
                jsonMapper,
                responseWriter,
                authMiddleware,
                roleGuard,
                otpConfigService,
                userService,
                null
        );
    }

    public Router(
            HttpServer server,
            AuthService authService,
            JsonMapper jsonMapper,
            HttpResponseWriter responseWriter,
            AuthMiddleware authMiddleware,
            RoleGuard roleGuard,
            OtpConfigService otpConfigService,
            UserService userService,
            OtpService otpService
    ) {
        this.server = server;
        this.authService = authService;
        this.jsonMapper = jsonMapper;
        this.responseWriter = responseWriter;
        this.authMiddleware = authMiddleware;
        this.roleGuard = roleGuard;
        this.otpConfigService = otpConfigService;
        this.userService = userService;
        this.otpService = otpService;
    }

    public void registerRoutes() {
        server.createContext("/health", new HealthHandler());

        server.createContext("/api/auth", new AuthHandler(
                authService,
                jsonMapper,
                responseWriter
        ));

        if (otpConfigService != null && userService != null) {
            server.createContext("/api/admin", new AdminHandler(
                    authMiddleware,
                    roleGuard,
                    otpConfigService,
                    userService,
                    jsonMapper,
                    responseWriter
            ));
        } else {
            server.createContext("/api/admin", new AdminPingHandler(
                    authMiddleware,
                    roleGuard,
                    responseWriter
            ));
        }

        if (otpService != null) {
            server.createContext("/api/user/otp", new UserOtpHandler(
                    authMiddleware,
                    roleGuard,
                    otpService,
                    jsonMapper,
                    responseWriter
            ));
        }

        server.createContext("/api/user", new UserPingHandler(
                authMiddleware,
                roleGuard,
                responseWriter
        ));
    }
}