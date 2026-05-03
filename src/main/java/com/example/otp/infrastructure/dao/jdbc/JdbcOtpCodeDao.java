package com.example.otp.infrastructure.dao.jdbc;

import com.example.otp.domain.model.OtpCode;
import com.example.otp.domain.model.OtpStatus;
import com.example.otp.infrastructure.dao.OtpCodeDao;
import com.example.otp.infrastructure.db.ConnectionFactory;

import java.sql.*;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

public final class JdbcOtpCodeDao implements OtpCodeDao {

    private final ConnectionFactory connectionFactory;

    public JdbcOtpCodeDao(ConnectionFactory connectionFactory) {
        this.connectionFactory = connectionFactory;
    }

    @Override
    public OtpCode save(OtpCode otpCode) {
        String sql = """
                INSERT INTO otp_codes (
                    id,
                    user_id,
                    operation_id,
                    code_hash,
                    status,
                    expires_at,
                    created_at,
                    used_at
                )
                VALUES (?, ?, ?, ?, ?, ?, ?, ?)
                """;

        try (Connection connection = connectionFactory.createConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setObject(1, otpCode.id());
            statement.setObject(2, otpCode.userId());
            statement.setObject(3, otpCode.operationId());
            statement.setString(4, otpCode.codeHash());
            statement.setString(5, otpCode.status().name());
            statement.setTimestamp(6, Timestamp.from(otpCode.expiresAt()));
            statement.setTimestamp(7, Timestamp.from(otpCode.createdAt()));

            if (otpCode.usedAt() == null) {
                statement.setNull(8, Types.TIMESTAMP_WITH_TIMEZONE);
            } else {
                statement.setTimestamp(8, Timestamp.from(otpCode.usedAt()));
            }

            statement.executeUpdate();
            return otpCode;
        } catch (SQLException exception) {
            throw new IllegalStateException("Failed to save OTP code", exception);
        }
    }

    @Override
    public Optional<OtpCode> findActiveByUserIdAndOperationId(UUID userId, UUID operationId) {
        String sql = """
                SELECT id, user_id, operation_id, code_hash, status, expires_at, created_at, used_at
                FROM otp_codes
                WHERE user_id = ?
                AND operation_id = ?
                AND status = 'ACTIVE'
                ORDER BY created_at DESC
                LIMIT 1
                """;

        try (Connection connection = connectionFactory.createConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setObject(1, userId);
            statement.setObject(2, operationId);

            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    return Optional.of(mapOtpCode(resultSet));
                }

                return Optional.empty();
            }
        } catch (SQLException exception) {
            throw new IllegalStateException("Failed to find active OTP code", exception);
        }
    }

    @Override
    public void markUsed(UUID id, Instant usedAt) {
        String sql = """
                UPDATE otp_codes
                SET status = 'USED', used_at = ?
                WHERE id = ?
                """;

        try (Connection connection = connectionFactory.createConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setTimestamp(1, Timestamp.from(usedAt));
            statement.setObject(2, id);
            statement.executeUpdate();
        } catch (SQLException exception) {
            throw new IllegalStateException("Failed to mark OTP code as used", exception);
        }
    }

    @Override
    public void markExpired(UUID id) {
        String sql = """
                UPDATE otp_codes
                SET status = 'EXPIRED'
                WHERE id = ?
                """;

        try (Connection connection = connectionFactory.createConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setObject(1, id);
            statement.executeUpdate();
        } catch (SQLException exception) {
            throw new IllegalStateException("Failed to mark OTP code as expired", exception);
        }
    }

    @Override
    public int markExpiredCodes(Instant now) {
        String sql = """
                UPDATE otp_codes
                SET status = 'EXPIRED'
                WHERE status = 'ACTIVE'
                AND expires_at < ?
                """;

        try (Connection connection = connectionFactory.createConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setTimestamp(1, Timestamp.from(now));
            return statement.executeUpdate();
        } catch (SQLException exception) {
            throw new IllegalStateException("Failed to mark expired OTP codes", exception);
        }
    }

    @Override
    public void deleteByUserId(UUID userId) {
        String sql = """
                DELETE FROM otp_codes
                WHERE user_id = ?
                """;

        try (Connection connection = connectionFactory.createConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setObject(1, userId);
            statement.executeUpdate();
        } catch (SQLException exception) {
            throw new IllegalStateException("Failed to delete OTP codes by user id", exception);
        }
    }

    private OtpCode mapOtpCode(ResultSet resultSet) throws SQLException {
        Timestamp usedAtTimestamp = resultSet.getTimestamp("used_at");

        return new OtpCode(
                resultSet.getObject("id", UUID.class),
                resultSet.getObject("user_id", UUID.class),
                resultSet.getObject("operation_id", UUID.class),
                resultSet.getString("code_hash"),
                OtpStatus.valueOf(resultSet.getString("status")),
                resultSet.getTimestamp("expires_at").toInstant(),
                resultSet.getTimestamp("created_at").toInstant(),
                usedAtTimestamp == null ? null : usedAtTimestamp.toInstant()
        );
    }
}