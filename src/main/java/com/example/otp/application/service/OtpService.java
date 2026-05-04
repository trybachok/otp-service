package com.example.otp.application.service;

import com.example.otp.application.port.OtpCodeGenerator;
import com.example.otp.application.port.OtpMessage;
import com.example.otp.application.port.OtpSender;
import com.example.otp.application.port.PasswordHasher;
import com.example.otp.domain.exception.BadRequestException;
import com.example.otp.domain.model.Operation;
import com.example.otp.domain.model.OtpCode;
import com.example.otp.domain.model.OtpConfig;
import com.example.otp.domain.model.OtpStatus;
import com.example.otp.infrastructure.dao.OperationDao;
import com.example.otp.infrastructure.dao.OtpCodeDao;
import com.example.otp.infrastructure.dao.OtpConfigDao;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;
import java.util.UUID;

public final class OtpService {

    private static final Logger logger = LoggerFactory.getLogger(OtpService.class);

    private final OtpConfigDao otpConfigDao;
    private final OperationDao operationDao;
    private final OtpCodeDao otpCodeDao;
    private final OtpCodeGenerator otpCodeGenerator;
    private final PasswordHasher passwordHasher;
    private final OtpSender otpSender;

    public OtpService(
            OtpConfigDao otpConfigDao,
            OperationDao operationDao,
            OtpCodeDao otpCodeDao,
            OtpCodeGenerator otpCodeGenerator,
            PasswordHasher passwordHasher,
            OtpSender otpSender
    ) {
        this.otpConfigDao = otpConfigDao;
        this.operationDao = operationDao;
        this.otpCodeDao = otpCodeDao;
        this.otpCodeGenerator = otpCodeGenerator;
        this.passwordHasher = passwordHasher;
        this.otpSender = otpSender;
    }

    public OtpCode generate(UUID userId, String operationId, String description) {
        String normalizedOperationId = validateOperationId(operationId);
        String normalizedDescription = normalizeDescription(description);

        OtpConfig config = otpConfigDao.getConfig();
        String plainCode = otpCodeGenerator.generate(config.codeLength());

        Operation operation = operationDao
                .findByUserIdAndOperationId(userId, normalizedOperationId)
                .orElseGet(() -> createOperation(userId, normalizedOperationId, normalizedDescription));

        Instant now = Instant.now();
        Instant expiresAt = now.plusSeconds(config.ttlSeconds());

        OtpCode otpCode = new OtpCode(
                UUID.randomUUID(),
                userId,
                operation.id(),
                passwordHasher.hash(plainCode),
                OtpStatus.ACTIVE,
                expiresAt,
                now,
                null
        );

        OtpCode savedOtpCode = otpCodeDao.save(otpCode);

        otpSender.send(new OtpMessage(
                userId,
                normalizedOperationId,
                plainCode
        ));

        logger.info("OTP generated userId={} operationId={} otpId={} expiresAt={}",
                userId,
                normalizedOperationId,
                savedOtpCode.id(),
                savedOtpCode.expiresAt()
        );

        return savedOtpCode;
    }

    private Operation createOperation(UUID userId, String operationId, String description) {
        Operation operation = new Operation(
                UUID.randomUUID(),
                userId,
                operationId,
                description,
                Instant.now()
        );

        Operation savedOperation = operationDao.save(operation);

        logger.info("Operation created userId={} operationId={} operationDbId={}",
                userId,
                operationId,
                savedOperation.id()
        );

        return savedOperation;
    }

    private String validateOperationId(String operationId) {
        if (operationId == null || operationId.isBlank()) {
            throw new BadRequestException("Operation id is required");
        }

        String normalizedOperationId = operationId.trim();

        if (normalizedOperationId.length() > 150) {
            throw new BadRequestException("Operation id length must be less than or equal to 150 characters");
        }

        return normalizedOperationId;
    }

    private String normalizeDescription(String description) {
        if (description == null || description.isBlank()) {
            return null;
        }

        return description.trim();
    }
}