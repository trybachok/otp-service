package com.example.otp;

import com.example.otp.api.router.Router;
import com.example.otp.config.AppConfig;
import com.example.otp.infrastructure.db.DatabaseMigrator;
import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.net.InetSocketAddress;

public class Main {

    public static void main(String[] args) throws IOException {
        AppConfig appConfig = AppConfig.load();

        DatabaseMigrator databaseMigrator = new DatabaseMigrator(appConfig);
        databaseMigrator.migrate();

        HttpServer server = HttpServer.create(
                new InetSocketAddress(appConfig.serverPort()),
                0
        );

        Router router = new Router(server);
        router.registerRoutes();

        server.start();

        System.out.println("OTP service started on port " + appConfig.serverPort());
        System.out.println("Health check: http://localhost:" + appConfig.serverPort() + "/health");
    }
}