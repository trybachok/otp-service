package com.example.otp.api.middleware;

import com.example.otp.application.security.AuthenticatedUser;
import com.example.otp.domain.exception.ForbiddenException;
import com.example.otp.domain.model.Role;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class RoleGuardTest {

    private final RoleGuard roleGuard = new RoleGuard();

    @Test
    void requireRoleAllowsUserWithRequiredRole() {
        AuthenticatedUser user = new AuthenticatedUser(
                UUID.randomUUID(),
                "admin",
                Role.ADMIN
        );

        assertDoesNotThrow(() -> roleGuard.requireRole(user, Role.ADMIN));
    }

    @Test
    void requireRoleRejectsUserWithWrongRole() {
        AuthenticatedUser user = new AuthenticatedUser(
                UUID.randomUUID(),
                "user1",
                Role.USER
        );

        ForbiddenException exception = assertThrows(
                ForbiddenException.class,
                () -> roleGuard.requireRole(user, Role.ADMIN)
        );

        assertEquals("Access denied", exception.getMessage());
    }
}