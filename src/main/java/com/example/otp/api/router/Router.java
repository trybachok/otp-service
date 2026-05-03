package com.example.otp.api.router;

import com.example.otp.api.handler.AuthHandler;
import com.example.otp.api.handler.HealthHandler;
import com.example.otp.api.response.HttpResponseWriter;
import com.example.otp.application.service.AuthService;
import com.example.otp.infrastructure.json.JsonMapper;
import com.sun.net.httpserver.HttpServer;

public final class Router {

    private final HttpServer server;
    private final AuthService authService;
    private final JsonMapper jsonMapper;
    private final HttpResponseWriter responseWriter;

    public Router(
            HttpServer server,
            AuthService authService,
            JsonMapper jsonMapper,
            HttpResponseWriter responseWriter
    ) {
        this.server = server;
        this.authService = authService;
        this.jsonMapper = jsonMapper;
        this.responseWriter = responseWriter;
    }

    public void registerRoutes() {
        server.createContext("/health", new HealthHandler());
        server.createContext("/api/auth", new AuthHandler(
                authService,
                jsonMapper,
                responseWriter
        ));
    }
}