package com.example.otp.infrastructure.dao.jdbc;

import com.example.otp.domain.model.Operation;
import com.example.otp.infrastructure.dao.OperationDao;
import com.example.otp.infrastructure.db.ConnectionFactory;

import java.sql.*;
import java.util.Optional;
import java.util.UUID;

public final class JdbcOperationDao implements OperationDao {

    private final ConnectionFactory connectionFactory;

    public JdbcOperationDao(ConnectionFactory connectionFactory) {
        this.connectionFactory = connectionFactory;
    }

    @Override
    public Operation save(Operation operation) {
        String sql = """
                INSERT INTO operations (id, user_id, operation_id, description, created_at)
                VALUES (?, ?, ?, ?, ?)
                """;

        try (Connection connection = connectionFactory.createConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setObject(1, operation.id());
            statement.setObject(2, operation.userId());
            statement.setString(3, operation.operationId());
            statement.setString(4, operation.description());
            statement.setTimestamp(5, Timestamp.from(operation.createdAt()));

            statement.executeUpdate();
            return operation;
        } catch (SQLException exception) {
            throw new IllegalStateException("Failed to save operation", exception);
        }
    }

    @Override
    public Optional<Operation> findByUserIdAndOperationId(UUID userId, String operationId) {
        String sql = """
                SELECT id, user_id, operation_id, description, created_at
                FROM operations
                WHERE user_id = ?
                AND operation_id = ?
                """;

        try (Connection connection = connectionFactory.createConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setObject(1, userId);
            statement.setString(2, operationId);

            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    return Optional.of(mapOperation(resultSet));
                }

                return Optional.empty();
            }
        } catch (SQLException exception) {
            throw new IllegalStateException("Failed to find operation", exception);
        }
    }

    @Override
    public void deleteByUserId(UUID userId) {
        String sql = """
                DELETE FROM operations
                WHERE user_id = ?
                """;

        try (Connection connection = connectionFactory.createConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setObject(1, userId);
            statement.executeUpdate();
        } catch (SQLException exception) {
            throw new IllegalStateException("Failed to delete operations by user id", exception);
        }
    }

    private Operation mapOperation(ResultSet resultSet) throws SQLException {
        return new Operation(
                resultSet.getObject("id", UUID.class),
                resultSet.getObject("user_id", UUID.class),
                resultSet.getString("operation_id"),
                resultSet.getString("description"),
                resultSet.getTimestamp("created_at").toInstant()
        );
    }
}