package com.example.otp.application.service;

import com.example.otp.application.port.PasswordHasher;
import com.example.otp.application.port.TokenProvider;
import com.example.otp.domain.exception.BadRequestException;
import com.example.otp.domain.exception.ConflictException;
import com.example.otp.domain.exception.UnauthorizedException;
import com.example.otp.domain.model.Role;
import com.example.otp.domain.model.User;
import com.example.otp.infrastructure.dao.UserDao;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;
import java.util.UUID;

public final class AuthService {

    private static final Logger logger = LoggerFactory.getLogger(AuthService.class);

    private final UserDao userDao;
    private final PasswordHasher passwordHasher;
    private final TokenProvider tokenProvider;
    private final long tokenTtlSeconds;

    public AuthService(
            UserDao userDao,
            PasswordHasher passwordHasher,
            TokenProvider tokenProvider,
            long tokenTtlSeconds
    ) {
        this.userDao = userDao;
        this.passwordHasher = passwordHasher;
        this.tokenProvider = tokenProvider;
        this.tokenTtlSeconds = tokenTtlSeconds;
    }

    public User register(String login, String password, String roleValue) {
        String normalizedLogin = validateLogin(login);
        String normalizedPassword = validatePassword(password);
        Role role = validateRole(roleValue);

        if (userDao.existsByLogin(normalizedLogin)) {
            logger.warn("Registration failed: login already exists login={}", normalizedLogin);
            throw new ConflictException("User with this login already exists");
        }

        if (role == Role.ADMIN && userDao.existsAdmin()) {
            logger.warn("Registration failed: admin already exists login={}", normalizedLogin);
            throw new ConflictException("Admin user already exists");
        }

        User user = new User(
                UUID.randomUUID(),
                normalizedLogin,
                passwordHasher.hash(normalizedPassword),
                role,
                Instant.now()
        );

        User savedUser = userDao.save(user);

        logger.info("User registered successfully userId={} login={} role={}",
                savedUser.id(),
                savedUser.login(),
                savedUser.role()
        );

        return savedUser;
    }

    public String login(String login, String password) {
        String normalizedLogin = validateLogin(login);
        String normalizedPassword = validatePassword(password);

        User user = userDao.findByLogin(normalizedLogin)
                .orElseThrow(() -> {
                    logger.warn("Login failed: user not found login={}", normalizedLogin);
                    return new UnauthorizedException("Invalid login or password");
                });

        if (!passwordHasher.verify(normalizedPassword, user.passwordHash())) {
            logger.warn("Login failed: invalid password userId={} login={}", user.id(), user.login());
            throw new UnauthorizedException("Invalid login or password");
        }

        String token = tokenProvider.generate(user);

        logger.info("User logged in successfully userId={} login={} role={}",
                user.id(),
                user.login(),
                user.role()
        );

        return token;
    }

    public long tokenTtlSeconds() {
        return tokenTtlSeconds;
    }

    private String validateLogin(String login) {
        if (login == null || login.isBlank()) {
            throw new BadRequestException("Login is required");
        }

        String normalizedLogin = login.trim();

        if (normalizedLogin.length() < 3 || normalizedLogin.length() > 100) {
            throw new BadRequestException("Login length must be between 3 and 100 characters");
        }

        return normalizedLogin;
    }

    private String validatePassword(String password) {
        if (password == null || password.isBlank()) {
            throw new BadRequestException("Password is required");
        }

        if (password.length() < 6 || password.length() > 100) {
            throw new BadRequestException("Password length must be between 6 and 100 characters");
        }

        return password;
    }

    private Role validateRole(String roleValue) {
        if (roleValue == null || roleValue.isBlank()) {
            throw new BadRequestException("Role is required");
        }

        try {
            return Role.valueOf(roleValue.trim().toUpperCase());
        } catch (IllegalArgumentException exception) {
            throw new BadRequestException("Role must be ADMIN or USER");
        }
    }
}