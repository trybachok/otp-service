package com.example.otp.api.middleware;

import com.example.otp.application.port.TokenProvider;
import com.example.otp.application.security.AuthenticatedUser;
import com.example.otp.domain.exception.UnauthorizedException;
import com.example.otp.domain.model.Role;
import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpContext;
import com.sun.net.httpserver.HttpExchange;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.URI;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class AuthMiddlewareTest {

    private TokenProvider tokenProvider;
    private AuthMiddleware authMiddleware;
    private TestHttpExchange exchange;

    @BeforeEach
    void setUp() {
        tokenProvider = mock(TokenProvider.class);
        authMiddleware = new AuthMiddleware(tokenProvider);
        exchange = new TestHttpExchange();
    }

    @Test
    void authenticateReturnsAuthenticatedUserForValidBearerToken() {
        AuthenticatedUser expectedUser = new AuthenticatedUser(
                UUID.randomUUID(),
                "user1",
                Role.USER
        );

        exchange.getRequestHeaders().add("Authorization", "Bearer valid-token");

        when(tokenProvider.verify("valid-token")).thenReturn(expectedUser);

        AuthenticatedUser actualUser = authMiddleware.authenticate(exchange);

        assertEquals(expectedUser.userId(), actualUser.userId());
        assertEquals(expectedUser.login(), actualUser.login());
        assertEquals(expectedUser.role(), actualUser.role());

        verify(tokenProvider).verify("valid-token");
    }

    @Test
    void authenticateRejectsMissingAuthorizationHeader() {
        UnauthorizedException exception = assertThrows(
                UnauthorizedException.class,
                () -> authMiddleware.authenticate(exchange)
        );

        assertEquals("Authorization header is required", exception.getMessage());

        verify(tokenProvider, never()).verify(anyString());
    }

    @Test
    void authenticateRejectsInvalidAuthorizationHeaderFormat() {
        exchange.getRequestHeaders().add("Authorization", "valid-token");

        UnauthorizedException exception = assertThrows(
                UnauthorizedException.class,
                () -> authMiddleware.authenticate(exchange)
        );

        assertEquals("Authorization header must start with Bearer", exception.getMessage());

        verify(tokenProvider, never()).verify(anyString());
    }

    @Test
    void authenticateRejectsEmptyBearerToken() {
        exchange.getRequestHeaders().add("Authorization", "Bearer ");

        UnauthorizedException exception = assertThrows(
                UnauthorizedException.class,
                () -> authMiddleware.authenticate(exchange)
        );

        assertEquals("Token is required", exception.getMessage());

        verify(tokenProvider, never()).verify(anyString());
    }

    @Test
    void authenticatePropagatesTokenProviderUnauthorizedException() {
        exchange.getRequestHeaders().add("Authorization", "Bearer invalid-token");

        when(tokenProvider.verify("invalid-token"))
                .thenThrow(new UnauthorizedException("Invalid or expired token"));

        UnauthorizedException exception = assertThrows(
                UnauthorizedException.class,
                () -> authMiddleware.authenticate(exchange)
        );

        assertEquals("Invalid or expired token", exception.getMessage());

        verify(tokenProvider).verify("invalid-token");
    }

    private static final class TestHttpExchange extends HttpExchange {

        private final Headers requestHeaders = new Headers();
        private final Headers responseHeaders = new Headers();

        @Override
        public Headers getRequestHeaders() {
            return requestHeaders;
        }

        @Override
        public Headers getResponseHeaders() {
            return responseHeaders;
        }

        @Override
        public URI getRequestURI() {
            return URI.create("/test");
        }

        @Override
        public String getRequestMethod() {
            return "GET";
        }

        @Override
        public HttpContext getHttpContext() {
            return null;
        }

        @Override
        public void close() {
        }

        @Override
        public InputStream getRequestBody() {
            return InputStream.nullInputStream();
        }

        @Override
        public OutputStream getResponseBody() {
            return OutputStream.nullOutputStream();
        }

        @Override
        public void sendResponseHeaders(int responseCode, long responseLength) {
        }

        @Override
        public InetSocketAddress getRemoteAddress() {
            return new InetSocketAddress("localhost", 0);
        }

        @Override
        public int getResponseCode() {
            return 0;
        }

        @Override
        public InetSocketAddress getLocalAddress() {
            return new InetSocketAddress("localhost", 0);
        }

        @Override
        public String getProtocol() {
            return "HTTP/1.1";
        }

        @Override
        public Object getAttribute(String name) {
            return null;
        }

        @Override
        public void setAttribute(String name, Object value) {
        }

        @Override
        public void setStreams(InputStream inputStream, OutputStream outputStream) {
        }

        @Override
        public com.sun.net.httpserver.HttpPrincipal getPrincipal() {
            return null;
        }
    }
}