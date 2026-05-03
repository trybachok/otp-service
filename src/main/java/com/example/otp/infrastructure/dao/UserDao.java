package com.example.otp.infrastructure.dao;

import com.example.otp.domain.model.User;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface UserDao {

    User save(User user);

    Optional<User> findById(UUID id);

    Optional<User> findByLogin(String login);

    boolean existsByLogin(String login);

    boolean existsAdmin();

    List<User> findAllNonAdmins();

    void deleteById(UUID id);
}