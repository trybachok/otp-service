package com.example.otp.application.service;

import com.example.otp.domain.exception.BadRequestException;
import com.example.otp.domain.exception.NotFoundException;
import com.example.otp.domain.model.Role;
import com.example.otp.domain.model.User;
import com.example.otp.infrastructure.dao.UserDao;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.UUID;

public final class UserService {

    private static final Logger logger = LoggerFactory.getLogger(UserService.class);

    private final UserDao userDao;

    public UserService(UserDao userDao) {
        this.userDao = userDao;
    }

    public List<User> getAllNonAdminUsers() {
        List<User> users = userDao.findAllNonAdmins();

        logger.info("Non-admin users requested count={}", users.size());

        return users;
    }

    public void deleteUser(UUID userId) {
        User user = userDao.findById(userId)
                .orElseThrow(() -> new NotFoundException("User not found"));

        if (user.role() == Role.ADMIN) {
            logger.warn("Attempt to delete admin user userId={}", userId);
            throw new BadRequestException("Admin user cannot be deleted");
        }

        userDao.deleteById(userId);

        logger.info("User deleted userId={} login={}", user.id(), user.login());
    }
}