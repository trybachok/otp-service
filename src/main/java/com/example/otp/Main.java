package com.example.otp;

import com.example.otp.api.router.Router;
import com.example.otp.config.AppConfig;
import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.net.BindException;
import java.net.InetSocketAddress;

public class Main {

    public static void main(String[] args) throws IOException {
        AppConfig appConfig = AppConfig.load();

        HttpServer server;
        try {
            server = HttpServer.create(
                    new InetSocketAddress(appConfig.serverPort()),
                    0
            );
        } catch (BindException exception) {
            System.err.println("Cannot start OTP service: port " + appConfig.serverPort() + " is already in use.");
            System.err.println("Stop the process using that port or start this service with another port, for example:");
            System.err.println("  SERVER_PORT=8081 mvn exec:java -Dexec.mainClass=com.example.otp.Main");
            System.exit(1);
            return;
        }

        Router router = new Router(server);
        router.registerRoutes();

        server.start();

        System.out.println("OTP service started on port " + appConfig.serverPort());
        System.out.println("Health check: http://localhost:" + appConfig.serverPort() + "/health");
    }
}
