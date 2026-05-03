package com.example.otp.api.router;

import com.example.otp.api.handler.HealthHandler;
import com.sun.net.httpserver.HttpServer;

public final class Router {

    private final HttpServer server;

    public Router(HttpServer server) {
        this.server = server;
    }

    public void registerRoutes() {
        server.createContext("/health", new HealthHandler());
    }
}