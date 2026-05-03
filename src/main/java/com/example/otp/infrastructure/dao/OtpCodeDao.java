package com.example.otp.infrastructure.dao;

import com.example.otp.domain.model.OtpCode;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

public interface OtpCodeDao {

    OtpCode save(OtpCode otpCode);

    Optional<OtpCode> findActiveByUserIdAndOperationId(UUID userId, UUID operationId);

    void markUsed(UUID id, Instant usedAt);

    void markExpired(UUID id);

    int markExpiredCodes(Instant now);

    void deleteByUserId(UUID userId);
}