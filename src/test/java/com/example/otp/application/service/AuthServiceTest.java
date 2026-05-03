package com.example.otp.application.service;

import com.example.otp.application.port.PasswordHasher;
import com.example.otp.application.port.TokenProvider;
import com.example.otp.domain.exception.BadRequestException;
import com.example.otp.domain.exception.ConflictException;
import com.example.otp.domain.exception.UnauthorizedException;
import com.example.otp.domain.model.Role;
import com.example.otp.domain.model.User;
import com.example.otp.infrastructure.dao.UserDao;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class AuthServiceTest {

    private UserDao userDao;
    private PasswordHasher passwordHasher;
    private TokenProvider tokenProvider;
    private AuthService authService;

    @BeforeEach
    void setUp() {
        userDao = mock(UserDao.class);
        passwordHasher = mock(PasswordHasher.class);
        tokenProvider = mock(TokenProvider.class);

        authService = new AuthService(
                userDao,
                passwordHasher,
                tokenProvider,
                3600
        );
    }

    @Test
    void registerCreatesUserWithHashedPassword() {
        when(userDao.existsByLogin("user1")).thenReturn(false);
        when(passwordHasher.hash("user123")).thenReturn("hashed-password");
        when(userDao.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        User user = authService.register("user1", "user123", "USER");

        assertNotNull(user.id());
        assertEquals("user1", user.login());
        assertEquals("hashed-password", user.passwordHash());
        assertEquals(Role.USER, user.role());
        assertNotNull(user.createdAt());

        verify(userDao).save(any(User.class));
    }

    @Test
    void registerCreatesAdminWhenAdminDoesNotExist() {
        when(userDao.existsByLogin("admin")).thenReturn(false);
        when(userDao.existsAdmin()).thenReturn(false);
        when(passwordHasher.hash("admin123")).thenReturn("hashed-password");
        when(userDao.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        User user = authService.register("admin", "admin123", "ADMIN");

        assertEquals(Role.ADMIN, user.role());

        verify(userDao).existsAdmin();
        verify(userDao).save(any(User.class));
    }

    @Test
    void registerRejectsSecondAdmin() {
        when(userDao.existsByLogin("admin2")).thenReturn(false);
        when(userDao.existsAdmin()).thenReturn(true);

        ConflictException exception = assertThrows(
                ConflictException.class,
                () -> authService.register("admin2", "admin123", "ADMIN")
        );

        assertEquals("Admin user already exists", exception.getMessage());

        verify(userDao, never()).save(any(User.class));
    }

    @Test
    void registerRejectsDuplicateLogin() {
        when(userDao.existsByLogin("user1")).thenReturn(true);

        ConflictException exception = assertThrows(
                ConflictException.class,
                () -> authService.register("user1", "user123", "USER")
        );

        assertEquals("User with this login already exists", exception.getMessage());

        verify(userDao, never()).save(any(User.class));
    }

    @Test
    void registerRejectsInvalidRole() {
        BadRequestException exception = assertThrows(
                BadRequestException.class,
                () -> authService.register("user1", "user123", "MANAGER")
        );

        assertEquals("Role must be ADMIN or USER", exception.getMessage());

        verify(userDao, never()).save(any(User.class));
    }

    @Test
    void registerRejectsEmptyLogin() {
        BadRequestException exception = assertThrows(
                BadRequestException.class,
                () -> authService.register("", "user123", "USER")
        );

        assertEquals("Login is required", exception.getMessage());

        verify(userDao, never()).save(any(User.class));
    }

    @Test
    void registerRejectsShortPassword() {
        BadRequestException exception = assertThrows(
                BadRequestException.class,
                () -> authService.register("user1", "123", "USER")
        );

        assertEquals("Password length must be between 6 and 100 characters", exception.getMessage());

        verify(userDao, never()).save(any(User.class));
    }

    @Test
    void loginReturnsTokenForValidCredentials() {
        User user = new User(
                UUID.randomUUID(),
                "user1",
                "hashed-password",
                Role.USER,
                Instant.now()
        );

        when(userDao.findByLogin("user1")).thenReturn(Optional.of(user));
        when(passwordHasher.verify("user123", "hashed-password")).thenReturn(true);
        when(tokenProvider.generate(user)).thenReturn("jwt-token");

        String token = authService.login("user1", "user123");

        assertEquals("jwt-token", token);

        verify(tokenProvider).generate(user);
    }

    @Test
    void loginRejectsUnknownUser() {
        when(userDao.findByLogin("unknown")).thenReturn(Optional.empty());

        UnauthorizedException exception = assertThrows(
                UnauthorizedException.class,
                () -> authService.login("unknown", "user123")
        );

        assertEquals("Invalid login or password", exception.getMessage());

        verify(tokenProvider, never()).generate(any());
    }

    @Test
    void loginRejectsWrongPassword() {
        User user = new User(
                UUID.randomUUID(),
                "user1",
                "hashed-password",
                Role.USER,
                Instant.now()
        );

        when(userDao.findByLogin("user1")).thenReturn(Optional.of(user));
        when(passwordHasher.verify("wrong-password", "hashed-password")).thenReturn(false);

        UnauthorizedException exception = assertThrows(
                UnauthorizedException.class,
                () -> authService.login("user1", "wrong-password")
        );

        assertEquals("Invalid login or password", exception.getMessage());

        verify(tokenProvider, never()).generate(any());
    }

    @Test
    void tokenTtlSecondsReturnsConfiguredValue() {
        assertEquals(3600, authService.tokenTtlSeconds());
    }
}