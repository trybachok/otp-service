package com.example.otp;

import com.example.otp.api.response.HttpResponseWriter;
import com.example.otp.api.router.Router;
import com.example.otp.application.port.PasswordHasher;
import com.example.otp.application.service.AuthService;
import com.example.otp.config.AppConfig;
import com.example.otp.infrastructure.dao.UserDao;
import com.example.otp.infrastructure.dao.jdbc.JdbcUserDao;
import com.example.otp.infrastructure.db.ConnectionFactory;
import com.example.otp.infrastructure.db.DatabaseMigrator;
import com.example.otp.infrastructure.json.JsonMapper;
import com.example.otp.infrastructure.security.BCryptPasswordHasher;
import com.sun.net.httpserver.HttpServer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.InetSocketAddress;

public class Main {

    private static final Logger logger = LoggerFactory.getLogger(Main.class);

    public static void main(String[] args) throws IOException {
        AppConfig appConfig = AppConfig.load();

        DatabaseMigrator databaseMigrator = new DatabaseMigrator(appConfig);
        databaseMigrator.migrate();

        ConnectionFactory connectionFactory = new ConnectionFactory(appConfig);

        UserDao userDao = new JdbcUserDao(connectionFactory);
        PasswordHasher passwordHasher = new BCryptPasswordHasher();

        AuthService authService = new AuthService(userDao, passwordHasher);

        JsonMapper jsonMapper = new JsonMapper();
        HttpResponseWriter responseWriter = new HttpResponseWriter(jsonMapper);

        HttpServer server = HttpServer.create(
                new InetSocketAddress(appConfig.serverPort()),
                0
        );

        Router router = new Router(
                server,
                authService,
                jsonMapper,
                responseWriter
        );

        router.registerRoutes();

        server.start();

        logger.info("OTP service started on port {}", appConfig.serverPort());
        logger.info("Health check: http://localhost:{}/health", appConfig.serverPort());
    }
}