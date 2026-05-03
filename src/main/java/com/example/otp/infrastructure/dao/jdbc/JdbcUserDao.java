package com.example.otp.infrastructure.dao.jdbc;

import com.example.otp.domain.model.Role;
import com.example.otp.domain.model.User;
import com.example.otp.infrastructure.dao.UserDao;
import com.example.otp.infrastructure.db.ConnectionFactory;

import java.sql.*;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public final class JdbcUserDao implements UserDao {

    private final ConnectionFactory connectionFactory;

    public JdbcUserDao(ConnectionFactory connectionFactory) {
        this.connectionFactory = connectionFactory;
    }

    @Override
    public User save(User user) {
        String sql = """
                INSERT INTO users (id, login, password_hash, role, created_at)
                VALUES (?, ?, ?, ?, ?)
                """;

        try (Connection connection = connectionFactory.createConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setObject(1, user.id());
            statement.setString(2, user.login());
            statement.setString(3, user.passwordHash());
            statement.setString(4, user.role().name());
            statement.setTimestamp(5, Timestamp.from(user.createdAt()));

            statement.executeUpdate();
            return user;
        } catch (SQLException exception) {
            throw new IllegalStateException("Failed to save user", exception);
        }
    }

    @Override
    public Optional<User> findById(UUID id) {
        String sql = """
                SELECT id, login, password_hash, role, created_at
                FROM users
                WHERE id = ?
                """;

        try (Connection connection = connectionFactory.createConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setObject(1, id);

            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    return Optional.of(mapUser(resultSet));
                }

                return Optional.empty();
            }
        } catch (SQLException exception) {
            throw new IllegalStateException("Failed to find user by id", exception);
        }
    }

    @Override
    public Optional<User> findByLogin(String login) {
        String sql = """
                SELECT id, login, password_hash, role, created_at
                FROM users
                WHERE login = ?
                """;

        try (Connection connection = connectionFactory.createConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setString(1, login);

            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    return Optional.of(mapUser(resultSet));
                }

                return Optional.empty();
            }
        } catch (SQLException exception) {
            throw new IllegalStateException("Failed to find user by login", exception);
        }
    }

    @Override
    public boolean existsByLogin(String login) {
        String sql = """
                SELECT EXISTS (
                    SELECT 1 FROM users WHERE login = ?
                )
                """;

        try (Connection connection = connectionFactory.createConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setString(1, login);

            try (ResultSet resultSet = statement.executeQuery()) {
                resultSet.next();
                return resultSet.getBoolean(1);
            }
        } catch (SQLException exception) {
            throw new IllegalStateException("Failed to check user login existence", exception);
        }
    }

    @Override
    public boolean existsAdmin() {
        String sql = """
                SELECT EXISTS (
                    SELECT 1 FROM users WHERE role = 'ADMIN'
                )
                """;

        try (Connection connection = connectionFactory.createConnection();
             PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet resultSet = statement.executeQuery()) {

            resultSet.next();
            return resultSet.getBoolean(1);
        } catch (SQLException exception) {
            throw new IllegalStateException("Failed to check admin existence", exception);
        }
    }

    @Override
    public List<User> findAllNonAdmins() {
        String sql = """
                SELECT id, login, password_hash, role, created_at
                FROM users
                WHERE role <> 'ADMIN'
                ORDER BY created_at DESC
                """;

        try (Connection connection = connectionFactory.createConnection();
             PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet resultSet = statement.executeQuery()) {

            List<User> users = new ArrayList<>();

            while (resultSet.next()) {
                users.add(mapUser(resultSet));
            }

            return users;
        } catch (SQLException exception) {
            throw new IllegalStateException("Failed to find non-admin users", exception);
        }
    }

    @Override
    public void deleteById(UUID id) {
        String sql = """
                DELETE FROM users
                WHERE id = ?
                """;

        try (Connection connection = connectionFactory.createConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setObject(1, id);
            statement.executeUpdate();
        } catch (SQLException exception) {
            throw new IllegalStateException("Failed to delete user", exception);
        }
    }

    private User mapUser(ResultSet resultSet) throws SQLException {
        return new User(
                resultSet.getObject("id", UUID.class),
                resultSet.getString("login"),
                resultSet.getString("password_hash"),
                Role.valueOf(resultSet.getString("role")),
                resultSet.getTimestamp("created_at").toInstant()
        );
    }
}