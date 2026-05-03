package com.example.otp.api;

import com.example.otp.api.middleware.AuthMiddleware;
import com.example.otp.api.middleware.RoleGuard;
import com.example.otp.api.response.HttpResponseWriter;
import com.example.otp.api.router.Router;
import com.example.otp.application.port.PasswordHasher;
import com.example.otp.application.port.TokenProvider;
import com.example.otp.application.service.AuthService;
import com.example.otp.config.AppConfig;
import com.example.otp.domain.model.Role;
import com.example.otp.domain.model.User;
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
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import static org.junit.jupiter.api.Assertions.*;

class AuthApiIntegrationTest {

    private HttpServer server;
    private String baseUrl;
    private HttpClient httpClient;
    private ObjectMapper objectMapper;
    private InMemoryUserDao userDao;

    @BeforeEach
    void setUp() throws IOException {
        httpClient = HttpClient.newHttpClient();
        objectMapper = new ObjectMapper();

        userDao = new InMemoryUserDao();

        AppConfig appConfig = AppConfig.load();

        PasswordHasher passwordHasher = new BCryptPasswordHasher();
        TokenProvider tokenProvider = new JwtTokenProvider(appConfig);

        AuthService authService = new AuthService(
                userDao,
                passwordHasher,
                tokenProvider,
                appConfig.tokenTtlSeconds()
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
                roleGuard
        );

        router.registerRoutes();
        server.start();

        int port = server.getAddress().getPort();
        baseUrl = "http://localhost:" + port;
    }

    @AfterEach
    void tearDown() {
        if (server != null) {
            server.stop(0);
        }
    }

    @Test
    void registerCreatesAdmin() throws Exception {
        HttpResponse<String> response = register("admin", "admin123", "ADMIN");

        assertEquals(201, response.statusCode());

        JsonNode json = json(response.body());
        assertEquals("admin", json.get("login").asText());
        assertEquals("ADMIN", json.get("role").asText());
        assertTrue(json.hasNonNull("id"));
    }

    @Test
    void secondAdminCannotBeCreated() throws Exception {
        register("admin", "admin123", "ADMIN");

        HttpResponse<String> response = register("admin2", "admin123", "ADMIN");

        assertEquals(409, response.statusCode());

        JsonNode json = json(response.body());
        assertEquals("CONFLICT", json.get("error").asText());
        assertEquals("Admin user already exists", json.get("message").asText());
    }

    @Test
    void registerCreatesUser() throws Exception {
        HttpResponse<String> response = register("user1", "user123", "USER");

        assertEquals(201, response.statusCode());

        JsonNode json = json(response.body());
        assertEquals("user1", json.get("login").asText());
        assertEquals("USER", json.get("role").asText());
    }

    @Test
    void duplicateLoginCannotBeRegistered() throws Exception {
        register("user1", "user123", "USER");

        HttpResponse<String> response = register("user1", "user123", "USER");

        assertEquals(409, response.statusCode());

        JsonNode json = json(response.body());
        assertEquals("CONFLICT", json.get("error").asText());
        assertEquals("User with this login already exists", json.get("message").asText());
    }

    @Test
    void passwordIsStoredAsHashNotPlainText() throws Exception {
        register("user1", "user123", "USER");

        User savedUser = userDao.findByLogin("user1").orElseThrow();

        assertNotEquals("user123", savedUser.passwordHash());
        assertTrue(savedUser.passwordHash().startsWith("$2"));
    }

    @Test
    void loginReturnsTokenTokenTypeAndExpiresInSeconds() throws Exception {
        register("admin", "admin123", "ADMIN");

        HttpResponse<String> response = login("admin", "admin123");

        assertEquals(200, response.statusCode());

        JsonNode json = json(response.body());
        assertTrue(json.hasNonNull("token"));
        assertEquals("Bearer", json.get("tokenType").asText());
        assertEquals(3600, json.get("expiresInSeconds").asLong());
    }

    @Test
    void wrongPasswordReturnsUnauthorized() throws Exception {
        register("admin", "admin123", "ADMIN");

        HttpResponse<String> response = login("admin", "wrong-password");

        assertEquals(401, response.statusCode());

        JsonNode json = json(response.body());
        assertEquals("UNAUTHORIZED", json.get("error").asText());
        assertEquals("Invalid login or password", json.get("message").asText());
    }

    @Test
    void unknownUserReturnsUnauthorized() throws Exception {
        HttpResponse<String> response = login("unknown", "admin123");

        assertEquals(401, response.statusCode());

        JsonNode json = json(response.body());
        assertEquals("UNAUTHORIZED", json.get("error").asText());
        assertEquals("Invalid login or password", json.get("message").asText());
    }

    @Test
    void invalidJsonReturnsBadRequest() throws Exception {
        HttpResponse<String> response = post("/api/auth/login", "{ bad json }");

        assertEquals(400, response.statusCode());

        JsonNode json = json(response.body());
        assertEquals("BAD_REQUEST", json.get("error").asText());
        assertEquals("Invalid JSON request body", json.get("message").asText());
    }

    @Test
    void unsupportedMethodReturnsMethodNotAllowed() throws Exception {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(baseUrl + "/api/auth/login"))
                .GET()
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

        assertEquals(405, response.statusCode());

        JsonNode json = json(response.body());
        assertEquals("METHOD_NOT_ALLOWED", json.get("error").asText());
    }

    @Test
    void invalidRoleReturnsBadRequest() throws Exception {
        HttpResponse<String> response = register("manager1", "user123", "MANAGER");

        assertEquals(400, response.statusCode());

        JsonNode json = json(response.body());
        assertEquals("BAD_REQUEST", json.get("error").asText());
        assertEquals("Role must be ADMIN or USER", json.get("message").asText());
    }

    @Test
    void emptyLoginReturnsBadRequest() throws Exception {
        HttpResponse<String> response = register("", "user123", "USER");

        assertEquals(400, response.statusCode());

        JsonNode json = json(response.body());
        assertEquals("BAD_REQUEST", json.get("error").asText());
        assertEquals("Login is required", json.get("message").asText());
    }

    @Test
    void adminTokenGivesAccessToAdminPing() throws Exception {
        register("admin", "admin123", "ADMIN");
        String adminToken = token("admin", "admin123");

        HttpResponse<String> response = get("/api/admin/ping", adminToken);

        assertEquals(200, response.statusCode());

        JsonNode json = json(response.body());
        assertEquals("OK", json.get("status").asText());
        assertEquals("ADMIN", json.get("role").asText());
        assertEquals("Admin access granted", json.get("message").asText());
    }

    @Test
    void userTokenDoesNotGiveAccessToAdminPing() throws Exception {
        register("user1", "user123", "USER");
        String userToken = token("user1", "user123");

        HttpResponse<String> response = get("/api/admin/ping", userToken);

        assertEquals(403, response.statusCode());

        JsonNode json = json(response.body());
        assertEquals("FORBIDDEN", json.get("error").asText());
        assertEquals("Access denied", json.get("message").asText());
    }

    @Test
    void userTokenGivesAccessToUserPing() throws Exception {
        register("user1", "user123", "USER");
        String userToken = token("user1", "user123");

        HttpResponse<String> response = get("/api/user/ping", userToken);

        assertEquals(200, response.statusCode());

        JsonNode json = json(response.body());
        assertEquals("OK", json.get("status").asText());
        assertEquals("USER", json.get("role").asText());
        assertEquals("User access granted", json.get("message").asText());
    }

    @Test
    void requestWithoutTokenReturnsUnauthorized() throws Exception {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(baseUrl + "/api/admin/ping"))
                .GET()
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

        assertEquals(401, response.statusCode());

        JsonNode json = json(response.body());
        assertEquals("UNAUTHORIZED", json.get("error").asText());
        assertEquals("Authorization header is required", json.get("message").asText());
    }

    @Test
    void invalidTokenReturnsUnauthorized() throws Exception {
        HttpResponse<String> response = get("/api/admin/ping", "invalid-token");

        assertEquals(401, response.statusCode());

        JsonNode json = json(response.body());
        assertEquals("UNAUTHORIZED", json.get("error").asText());
        assertEquals("Invalid or expired token", json.get("message").asText());
    }

    @Test
    void missingBearerPrefixReturnsUnauthorized() throws Exception {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(baseUrl + "/api/admin/ping"))
                .header("Authorization", "invalid-format")
                .GET()
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

        assertEquals(401, response.statusCode());

        JsonNode json = json(response.body());
        assertEquals("UNAUTHORIZED", json.get("error").asText());
        assertEquals("Authorization header must start with Bearer", json.get("message").asText());
    }

    @Test
    void unsuitableRoleReturnsForbidden() throws Exception {
        register("admin", "admin123", "ADMIN");
        String adminToken = token("admin", "admin123");

        HttpResponse<String> response = get("/api/user/ping", adminToken);

        assertEquals(403, response.statusCode());

        JsonNode json = json(response.body());
        assertEquals("FORBIDDEN", json.get("error").asText());
        assertEquals("Access denied", json.get("message").asText());
    }

    private HttpResponse<String> register(String login, String password, String role) throws Exception {
        String body = """
                {
                  "login": "%s",
                  "password": "%s",
                  "role": "%s"
                }
                """.formatted(login, password, role);

        return post("/api/auth/register", body);
    }

    private HttpResponse<String> login(String login, String password) throws Exception {
        String body = """
                {
                  "login": "%s",
                  "password": "%s"
                }
                """.formatted(login, password);

        return post("/api/auth/login", body);
    }

    private String token(String login, String password) throws Exception {
        HttpResponse<String> response = login(login, password);

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

    private HttpResponse<String> get(String path, String token) throws Exception {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(baseUrl + path))
                .header("Authorization", "Bearer " + token)
                .GET()
                .build();

        return httpClient.send(request, HttpResponse.BodyHandlers.ofString());
    }

    private JsonNode json(String body) throws Exception {
        return objectMapper.readTree(body);
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