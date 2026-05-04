package com.example.otp;

import com.example.otp.api.middleware.AuthMiddleware;
import com.example.otp.api.middleware.RoleGuard;
import com.example.otp.api.response.HttpResponseWriter;
import com.example.otp.api.router.Router;
import com.example.otp.application.port.OtpCodeGenerator;
import com.example.otp.application.port.OtpSender;
import com.example.otp.application.port.PasswordHasher;
import com.example.otp.application.port.TokenProvider;
import com.example.otp.application.scheduler.OtpExpirationScheduler;
import com.example.otp.application.service.AuthService;
import com.example.otp.application.service.OtpConfigService;
import com.example.otp.application.service.OtpService;
import com.example.otp.application.service.UserService;
import com.example.otp.config.AppConfig;
import com.example.otp.infrastructure.dao.OperationDao;
import com.example.otp.infrastructure.dao.OtpCodeDao;
import com.example.otp.infrastructure.dao.OtpConfigDao;
import com.example.otp.infrastructure.dao.UserDao;
import com.example.otp.infrastructure.dao.jdbc.JdbcOperationDao;
import com.example.otp.infrastructure.dao.jdbc.JdbcOtpCodeDao;
import com.example.otp.infrastructure.dao.jdbc.JdbcOtpConfigDao;
import com.example.otp.infrastructure.dao.jdbc.JdbcUserDao;
import com.example.otp.infrastructure.db.ConnectionFactory;
import com.example.otp.infrastructure.db.DatabaseMigrator;
import com.example.otp.infrastructure.json.JsonMapper;
import com.example.otp.infrastructure.security.BCryptPasswordHasher;
import com.example.otp.infrastructure.security.JwtTokenProvider;
import com.example.otp.infrastructure.security.SecureRandomOtpCodeGenerator;
import com.example.otp.infrastructure.sender.CompositeOtpSender;
import com.example.otp.infrastructure.sender.EmailOtpSender;
import com.example.otp.infrastructure.sender.FileOtpSender;
import com.example.otp.infrastructure.sender.SmsOtpSender;
import com.example.otp.infrastructure.sender.TelegramOtpSender;
import com.sun.net.httpserver.HttpServer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.file.Path;
import java.util.List;

public class Main {

    private static final Logger logger = LoggerFactory.getLogger(Main.class);

    public static void main(String[] args) throws IOException {
        AppConfig appConfig = AppConfig.load();

        DatabaseMigrator databaseMigrator = new DatabaseMigrator(appConfig);
        databaseMigrator.migrate();

        ConnectionFactory connectionFactory = new ConnectionFactory(appConfig);

        UserDao userDao = new JdbcUserDao(connectionFactory);
        OtpConfigDao otpConfigDao = new JdbcOtpConfigDao(connectionFactory);
        OperationDao operationDao = new JdbcOperationDao(connectionFactory);
        OtpCodeDao otpCodeDao = new JdbcOtpCodeDao(connectionFactory);

        PasswordHasher passwordHasher = new BCryptPasswordHasher();
        TokenProvider tokenProvider = new JwtTokenProvider(appConfig);
        OtpCodeGenerator otpCodeGenerator = new SecureRandomOtpCodeGenerator();

        List<OtpSender> senders = List.of(
                new FileOtpSender(Path.of("otp-codes.txt")),
                new SmsOtpSender(),
                new EmailOtpSender(),
                new TelegramOtpSender()
        );

        CompositeOtpSender otpSender = new CompositeOtpSender(senders);

        AuthService authService = new AuthService(
                userDao,
                passwordHasher,
                tokenProvider,
                appConfig.tokenTtlSeconds()
        );

        OtpConfigService otpConfigService = new OtpConfigService(otpConfigDao);
        UserService userService = new UserService(userDao);

        OtpService otpService = new OtpService(
                otpConfigDao,
                operationDao,
                otpCodeDao,
                otpCodeGenerator,
                passwordHasher,
                otpSender
        );

        OtpExpirationScheduler otpExpirationScheduler = new OtpExpirationScheduler(
                otpCodeDao,
                appConfig.otpExpirationSchedulerIntervalSeconds()
        );

        AuthMiddleware authMiddleware = new AuthMiddleware(tokenProvider);
        RoleGuard roleGuard = new RoleGuard();

        JsonMapper jsonMapper = new JsonMapper();
        HttpResponseWriter responseWriter = new HttpResponseWriter(jsonMapper);

        HttpServer server = HttpServer.create(new InetSocketAddress(appConfig.serverPort()), 0);

        Router router = new Router(
                server,
                authService,
                jsonMapper,
                responseWriter,
                authMiddleware,
                roleGuard,
                otpConfigService,
                userService,
                otpService
        );

        router.registerRoutes();

        server.start();
        otpExpirationScheduler.start();

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            logger.info("Shutdown started");
            server.stop(0);
            otpExpirationScheduler.stop();
            logger.info("Shutdown completed");
        }));

        logger.info("OTP service started on port {}", appConfig.serverPort());
        logger.info("Health check: http://localhost:{}/health", appConfig.serverPort());
    }
}