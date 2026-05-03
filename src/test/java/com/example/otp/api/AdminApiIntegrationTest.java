package com.example.otp.api;

import com.example.otp.api.middleware.AuthMiddleware;
import com.example.otp.api.middleware.RoleGuard;
import com.example.otp.api.response.HttpResponseWriter;
import com.example.otp.api.router.Router;
import com.example.otp.application.port.PasswordHasher;
import com.example.otp.application.port.TokenProvider;
import com.example.otp.application.service.AuthService;
import com.example.otp.application.service.OtpConfigService;
import com.example.otp.application.service.UserService;
import com.example.otp.config.AppConfig;
import com.example.otp.domain.model.OtpConfig;
import com.example.otp.domain.model.Role;
import com.example.otp.domain.model.User;
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

class AdminApiIntegrationTest {

    private HttpServer server;
    private String baseUrl;
    private HttpClient httpClient;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() throws IOException {
        httpClient = HttpClient.newHttpClient();
        objectMapper = new ObjectMapper();

        AppConfig appConfig = AppConfig.load();

        InMemoryUserDao userDao = new InMemoryUserDao();
        InMemoryOtpConfigDao otpConfigDao = new InMemoryOtpConfigDao();

        PasswordHasher passwordHasher = new BCryptPasswordHasher();
        TokenProvider tokenProvider = new JwtTokenProvider(appConfig);

        AuthService authService = new AuthService(
                userDao,
                passwordHasher,
                tokenProvider,
                appConfig.tokenTtlSeconds()
        );

        OtpConfigService otpConfigService = new OtpConfigService(otpConfigDao);
        UserService userService = new UserService(userDao);

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
                userService
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
    void adminCanUpdateOtpConfig() throws Exception {
        register("admin", "admin123", "ADMIN");
        String adminToken = token("admin", "admin123");

        HttpResponse<String> response = put(
                "/api/admin/otp-config",
                """
                {
                  "codeLength": 8,
                  "ttlSeconds": 600
                }
                """,
                adminToken
        );

        assertEquals(200, response.statusCode());

        JsonNode json = json(response.body());
        assertEquals(8, json.get("codeLength").asInt());
        assertEquals(600, json.get("ttlSeconds").asInt());
    }

    @Test
    void userCannotUpdateOtpConfig() throws Exception {
        register("user1", "user123", "USER");
        String userToken = token("user1", "user123");

        HttpResponse<String> response = put(
                "/api/admin/otp-config",
                """
                {
                  "codeLength": 8,
                  "ttlSeconds": 600
                }
                """,
                userToken
        );

        assertEquals(403, response.statusCode());

        JsonNode json = json(response.body());
        assertEquals("FORBIDDEN", json.get("error").asText());
    }

    @Test
    void adminCanGetUsersWithoutAdmins() throws Exception {
        register("admin", "admin123", "ADMIN");
        register("user1", "user123", "USER");
        register("user2", "user123", "USER");

        String adminToken = token("admin", "admin123");

        HttpResponse<String> response = get("/api/admin/users", adminToken);

        assertEquals(200, response.statusCode());

        JsonNode json = json(response.body());
        assertEquals(2, json.size());
        assertEquals("USER", json.get(0).get("role").asText());
        assertEquals("USER", json.get(1).get("role").asText());
    }

    @Test
    void adminCanDeleteUser() throws Exception {
        register("admin", "admin123", "ADMIN");
        register("user1", "user123", "USER");

        String adminToken = token("admin", "admin123");

        JsonNode users = json(get("/api/admin/users", adminToken).body());
        String userId = users.get(0).get("id").asText();

        HttpResponse<String> response = delete("/api/admin/users/" + userId, adminToken);

        assertEquals(200, response.statusCode());

        JsonNode usersAfterDelete = json(get("/api/admin/users", adminToken).body());
        assertEquals(0, usersAfterDelete.size());
    }

    @Test
    void adminCannotDeleteUnknownUser() throws Exception {
        register("admin", "admin123", "ADMIN");
        String adminToken = token("admin", "admin123");

        HttpResponse<String> response = delete(
                "/api/admin/users/" + UUID.randomUUID(),
                adminToken
        );

        assertEquals(404, response.statusCode());

        JsonNode json = json(response.body());
        assertEquals("NOT_FOUND", json.get("error").asText());
        assertEquals("User not found", json.get("message").asText());
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

    private HttpResponse<String> put(String path, String body, String token) throws Exception {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(baseUrl + path))
                .header("Content-Type", "application/json")
                .header("Authorization", "Bearer " + token)
                .PUT(HttpRequest.BodyPublishers.ofString(body))
                .build();

        return httpClient.send(request, HttpResponse.BodyHandlers.ofString());
    }

    private HttpResponse<String> get(String path, String token) throws Exception {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(baseUrl + path))
                .header("Authorization", "Bearer " + token)
                .GET()
                .build();

        return httpClient.send(request, HttpResponse.BodyHandlers.ofString());
    }

    private HttpResponse<String> delete(String path, String token) throws Exception {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(baseUrl + path))
                .header("Authorization", "Bearer " + token)
                .DELETE()
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
}