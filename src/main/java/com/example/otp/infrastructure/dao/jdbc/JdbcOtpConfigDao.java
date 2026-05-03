package com.example.otp.infrastructure.dao.jdbc;

import com.example.otp.domain.model.OtpConfig;
import com.example.otp.infrastructure.dao.OtpConfigDao;
import com.example.otp.infrastructure.db.ConnectionFactory;

import java.sql.*;

public final class JdbcOtpConfigDao implements OtpConfigDao {

    private final ConnectionFactory connectionFactory;

    public JdbcOtpConfigDao(ConnectionFactory connectionFactory) {
        this.connectionFactory = connectionFactory;
    }

    @Override
    public OtpConfig getConfig() {
        String sql = """
                SELECT id, code_length, ttl_seconds, updated_at
                FROM otp_config
                WHERE id = 1
                """;

        try (Connection connection = connectionFactory.createConnection();
             PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet resultSet = statement.executeQuery()) {

            if (!resultSet.next()) {
                throw new IllegalStateException("OTP config not found");
            }

            return mapOtpConfig(resultSet);
        } catch (SQLException exception) {
            throw new IllegalStateException("Failed to get OTP config", exception);
        }
    }

    @Override
    public OtpConfig updateConfig(int codeLength, int ttlSeconds) {
        String sql = """
                UPDATE otp_config
                SET code_length = ?, ttl_seconds = ?, updated_at = now()
                WHERE id = 1
                RETURNING id, code_length, ttl_seconds, updated_at
                """;

        try (Connection connection = connectionFactory.createConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setInt(1, codeLength);
            statement.setInt(2, ttlSeconds);

            try (ResultSet resultSet = statement.executeQuery()) {
                if (!resultSet.next()) {
                    throw new IllegalStateException("OTP config not found");
                }

                return mapOtpConfig(resultSet);
            }
        } catch (SQLException exception) {
            throw new IllegalStateException("Failed to update OTP config", exception);
        }
    }

    private OtpConfig mapOtpConfig(ResultSet resultSet) throws SQLException {
        return new OtpConfig(
                resultSet.getInt("id"),
                resultSet.getInt("code_length"),
                resultSet.getInt("ttl_seconds"),
                resultSet.getTimestamp("updated_at").toInstant()
        );
    }
}