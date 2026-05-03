package com.example.otp.api.middleware;

import com.example.otp.application.security.AuthenticatedUser;
import com.example.otp.domain.exception.ForbiddenException;
import com.example.otp.domain.model.Role;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class RoleGuard {

    private static final Logger logger = LoggerFactory.getLogger(RoleGuard.class);

    public void requireRole(AuthenticatedUser user, Role requiredRole) {
        if (user.role() != requiredRole) {
            logger.warn("Access denied userId={} actualRole={} requiredRole={}",
                    user.userId(),
                    user.role(),
                    requiredRole
            );

            throw new ForbiddenException("Access denied");
        }
    }
}