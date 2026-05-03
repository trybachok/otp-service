package com.example.otp.application.service;

import com.example.otp.domain.exception.BadRequestException;
import com.example.otp.domain.exception.NotFoundException;
import com.example.otp.domain.model.Role;
import com.example.otp.domain.model.User;
import com.example.otp.infrastructure.dao.UserDao;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class UserServiceTest {

    private UserDao userDao;
    private UserService service;

    @BeforeEach
    void setUp() {
        userDao = mock(UserDao.class);
        service = new UserService(userDao);
    }

    @Test
    void getAllNonAdminUsersReturnsUsersFromDao() {
        User user = new User(
                UUID.randomUUID(),
                "user1",
                "hash",
                Role.USER,
                Instant.now()
        );

        when(userDao.findAllNonAdmins()).thenReturn(List.of(user));

        List<User> result = service.getAllNonAdminUsers();

        assertEquals(1, result.size());
        assertEquals("user1", result.get(0).login());
    }

    @Test
    void deleteUserDeletesExistingUser() {
        UUID userId = UUID.randomUUID();

        User user = new User(
                userId,
                "user1",
                "hash",
                Role.USER,
                Instant.now()
        );

        when(userDao.findById(userId)).thenReturn(Optional.of(user));

        service.deleteUser(userId);

        verify(userDao).deleteById(userId);
    }

    @Test
    void deleteUserRejectsAdmin() {
        UUID adminId = UUID.randomUUID();

        User admin = new User(
                adminId,
                "admin",
                "hash",
                Role.ADMIN,
                Instant.now()
        );

        when(userDao.findById(adminId)).thenReturn(Optional.of(admin));

        BadRequestException exception = assertThrows(
                BadRequestException.class,
                () -> service.deleteUser(adminId)
        );

        assertEquals("Admin user cannot be deleted", exception.getMessage());

        verify(userDao, never()).deleteById(any());
    }

    @Test
    void deleteUserRejectsUnknownUser() {
        UUID userId = UUID.randomUUID();

        when(userDao.findById(userId)).thenReturn(Optional.empty());

        NotFoundException exception = assertThrows(
                NotFoundException.class,
                () -> service.deleteUser(userId)
        );

        assertEquals("User not found", exception.getMessage());

        verify(userDao, never()).deleteById(any());
    }
}