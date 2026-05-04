package com.example.otp.api;

import com.example.otp.api.middleware.AuthMiddleware;
import com.example.otp.api.middleware.RoleGuard;
import com.example.otp.api.response.HttpResponseWriter;
import com.example.otp.api.router.Router;
import com.example.otp.application.port.OtpCodeGenerator;
import com.example.otp.application.port.OtpSender;
import com.example.otp.application.port.PasswordHasher;
import com.example.otp.application.port.TokenProvider;
import com.example.otp.application.service.AuthService;
import com.example.otp.application.service.OtpConfigService;
import com.example.otp.application.service.OtpService;
import com.example.otp.application.service.UserService;
import com.example.otp.config.AppConfig;
import com.example.otp.domain.model.Operation;
import com.example.otp.domain.model.OtpCode;
import com.example.otp.domain.model.OtpConfig;
import com.example.otp.domain.model.Role;
import com.example.otp.domain.model.User;
import com.example.otp.infrastructure.dao.OperationDao;
import com.example.otp.infrastructure.dao.OtpCodeDao;
import com.example.otp.infrastructure.dao.OtpConfigDao;
import com.example.otp.infrastructure.dao.UserDao;
import com.example.otp.infrastructure.json.JsonMapper;
import com.example.otp.infrastructure.security.BCryptPasswordHasher;
import com.example.otp.infrastructure.security.JwtTokenProvider;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import static org.junit.jupiter.api.Assertions.*;

class UserOtpApiIntegrationTest {

    private HttpServer server;
    private String baseUrl;
    private HttpClient httpClient;
    private ObjectMapper objectMapper;
    private InMemoryOtpCodeDao otpCodeDao;

    @BeforeEach
    void setUp() throws IOException {
        httpClient = HttpClient.newHttpClient();
        objectMapper = new ObjectMapper();

        AppConfig appConfig = AppConfig.load();

        InMemoryUserDao userDao = new InMemoryUserDao();
        InMemoryOtpConfigDao otpConfigDao = new InMemoryOtpConfigDao();
        InMemoryOperationDao operationDao = new InMemoryOperationDao();
        otpCodeDao = new InMemoryOtpCodeDao();

        PasswordHasher passwordHasher = new BCryptPasswordHasher();
        TokenProvider tokenProvider = new JwtTokenProvider(appConfig);
        OtpCodeGenerator otpCodeGenerator = length -> "123456";
        OtpSender otpSender = message -> {
        };

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

        AuthMiddleware authMiddleware = new AuthMiddleware(tokenProvider);
        RoleGuard roleGuard = new RoleGuard();

        JsonMapper jsonMapper = new JsonMapper();
        HttpResponseWriter responseWriter = new HttpResponseWriter(jsonMapper);

        server = HttpServer.create(new InetSocketAddress("localhost", 0), 0);

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

        baseUrl = "http://localhost:" + server.getAddress().getPort();
    }

    @AfterEach
    void tearDown() {
        if (server != null) {
            server.stop(0);
        }
    }

    @Test
    void userCanGenerateOtp() throws Exception {
        register("user1", "user123", "USER");
        String userToken = token("user1", "user123");

        HttpResponse<String> response = postWithToken(
                "/api/user/otp/generate",
                """
                {
                  "operationId": "payment-123",
                  "description": "Confirm payment"
                }
                """,
                userToken
        );

        assertEquals(201, response.statusCode());

        JsonNode json = json(response.body());
        assertEquals("payment-123", json.get("operationId").asText());
        assertEquals("ACTIVE", json.get("status").asText());
        assertTrue(json.hasNonNull("otpId"));
        assertTrue(json.hasNonNull("operationDbId"));
        assertTrue(json.hasNonNull("expiresAt"));

        assertEquals(1, otpCodeDao.count());
    }

    @Test
    void adminCannotGenerateOtp() throws Exception {
        register("admin", "admin123", "ADMIN");
        String adminToken = token("admin", "admin123");

        HttpResponse<String> response = postWithToken(
                "/api/user/otp/generate",
                """
                {
                  "operationId": "payment-123",
                  "description": "Confirm payment"
                }
                """,
                adminToken
        );

        assertEquals(403, response.statusCode());

        JsonNode json = json(response.body());
        assertEquals("FORBIDDEN", json.get("error").asText());
    }

    @Test
    void generateOtpRejectsEmptyOperationId() throws Exception {
        register("user1", "user123", "USER");
        String userToken = token("user1", "user123");

        HttpResponse<String> response = postWithToken(
                "/api/user/otp/generate",
                """
                {
                  "operationId": "",
                  "description": "Confirm payment"
                }
                """,
                userToken
        );

        assertEquals(400, response.statusCode());

        JsonNode json = json(response.body());
        assertEquals("BAD_REQUEST", json.get("error").asText());
        assertEquals("Operation id is required", json.get("message").asText());
    }

    private HttpResponse<String> register(String login, String password, String role) throws Exception {
        return post(
                "/api/auth/register",
                """
                {
                  "login": "%s",
                  "password": "%s",
                  "role": "%s"
                }
                """.formatted(login, password, role)
        );
    }

    private String token(String login, String password) throws Exception {
        HttpResponse<String> response = post(
                "/api/auth/login",
                """
                {
                  "login": "%s",
                  "password": "%s"
                }
                """.formatted(login, password)
        );

        assertEquals(200, response.statusCode());

        return json(response.body()).get("token").asText();
    }

    private HttpResponse<String> post(String path, String body) throws Exception {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(baseUrl + path))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(body))
                .build();

        return httpClient.send(request, HttpResponse.BodyHandlers.ofString());
    }

    private HttpResponse<String> postWithToken(String path, String body, String token) throws Exception {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(baseUrl + path))
                .header("Content-Type", "application/json")
                .header("Authorization", "Bearer " + token)
                .POST(HttpRequest.BodyPublishers.ofString(body))
                .build();

        return httpClient.send(request, HttpResponse.BodyHandlers.ofString());
    }

    private JsonNode json(String body) throws Exception {
        return objectMapper.readTree(body);
    }

    private static final class InMemoryOtpConfigDao implements OtpConfigDao {

        private OtpConfig config = new OtpConfig(1, 6, 300, Instant.now());

        @Override
        public OtpConfig getConfig() {
            return config;
        }

        @Override
        public OtpConfig updateConfig(int codeLength, int ttlSeconds) {
            config = new OtpConfig(1, codeLength, ttlSeconds, Instant.now());
            return config;
        }
    }

    private static final class InMemoryUserDao implements UserDao {

        private final Map<UUID, User> usersById = new ConcurrentHashMap<>();
        private final Map<String, User> usersByLogin = new ConcurrentHashMap<>();

        @Override
        public User save(User user) {
            usersById.put(user.id(), user);
            usersByLogin.put(user.login(), user);
            return user;
        }

        @Override
        public Optional<User> findById(UUID id) {
            return Optional.ofNullable(usersById.get(id));
        }

        @Override
        public Optional<User> findByLogin(String login) {
            return Optional.ofNullable(usersByLogin.get(login));
        }

        @Override
        public boolean existsByLogin(String login) {
            return usersByLogin.containsKey(login);
        }

        @Override
        public boolean existsAdmin() {
            return usersById.values()
                    .stream()
                    .anyMatch(user -> user.role() == Role.ADMIN);
        }

        @Override
        public List<User> findAllNonAdmins() {
            return usersById.values()
                    .stream()
                    .filter(user -> user.role() != Role.ADMIN)
                    .toList();
        }

        @Override
        public void deleteById(UUID id) {
            User removedUser = usersById.remove(id);

            if (removedUser != null) {
                usersByLogin.remove(removedUser.login());
            }
        }
    }

    private static final class InMemoryOperationDao implements OperationDao {

        private final Map<UUID, Operation> operationsById = new ConcurrentHashMap<>();

        @Override
        public Operation save(Operation operation) {
            operationsById.put(operation.id(), operation);
            return operation;
        }

        @Override
        public Optional<Operation> findByUserIdAndOperationId(UUID userId, String operationId) {
            return operationsById.values()
                    .stream()
                    .filter(operation -> operation.userId().equals(userId))
                    .filter(operation -> operation.operationId().equals(operationId))
                    .findFirst();
        }

        @Override
        public void deleteByUserId(UUID userId) {
            operationsById.values().removeIf(operation -> operation.userId().equals(userId));
        }
    }

    private static final class InMemoryOtpCodeDao implements OtpCodeDao {

        private final Map<UUID, OtpCode> otpCodesById = new ConcurrentHashMap<>();

        @Override
        public OtpCode save(OtpCode otpCode) {
            otpCodesById.put(otpCode.id(), otpCode);
            return otpCode;
        }

        @Override
        public Optional<OtpCode> findActiveByUserIdAndOperationId(UUID userId, UUID operationId) {
            return otpCodesById.values()
                    .stream()
                    .filter(otpCode -> otpCode.userId().equals(userId))
                    .filter(otpCode -> otpCode.operationId().equals(operationId))
                    .findFirst();
        }

        @Override
        public void markUsed(UUID id, Instant usedAt) {
        }

        @Override
        public void markExpired(UUID id) {
        }

        @Override
        public int markExpiredCodes(Instant now) {
            return 0;
        }

        @Override
        public void deleteByUserId(UUID userId) {
            otpCodesById.values().removeIf(otpCode -> otpCode.userId().equals(userId));
        }

        int count() {
            return otpCodesById.size();
        }
    }
}