package com.example.otp.application.service;

import com.example.otp.api.dto.otp.DeliveryDestination;
import com.example.otp.application.port.OtpCodeGenerator;
import com.example.otp.application.port.OtpMessage;
import com.example.otp.application.port.PasswordHasher;
import com.example.otp.domain.exception.BadRequestException;
import com.example.otp.domain.exception.NotFoundException;
import com.example.otp.domain.model.Operation;
import com.example.otp.domain.model.OtpChannel;
import com.example.otp.domain.model.OtpCode;
import com.example.otp.domain.model.OtpConfig;
import com.example.otp.domain.model.OtpStatus;
import com.example.otp.infrastructure.dao.OperationDao;
import com.example.otp.infrastructure.dao.OtpCodeDao;
import com.example.otp.infrastructure.dao.OtpConfigDao;
import com.example.otp.infrastructure.sender.CompositeOtpSender;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public final class OtpService {

    private static final Logger logger = LoggerFactory.getLogger(OtpService.class);

    private final OtpConfigDao otpConfigDao;
    private final OperationDao operationDao;
    private final OtpCodeDao otpCodeDao;
    private final OtpCodeGenerator otpCodeGenerator;
    private final PasswordHasher passwordHasher;
    private final CompositeOtpSender otpSender;

    public OtpService(
            OtpConfigDao otpConfigDao,
            OperationDao operationDao,
            OtpCodeDao otpCodeDao,
            OtpCodeGenerator otpCodeGenerator,
            PasswordHasher passwordHasher,
            CompositeOtpSender otpSender
    ) {
        this.otpConfigDao = otpConfigDao;
        this.operationDao = operationDao;
        this.otpCodeDao = otpCodeDao;
        this.otpCodeGenerator = otpCodeGenerator;
        this.passwordHasher = passwordHasher;
        this.otpSender = otpSender;
    }

    public OtpCode generate(UUID userId, String operationId, String description) {
        return generate(userId, operationId, description, List.of("FILE"), null);
    }

    public OtpCode generate(
            UUID userId,
            String operationId,
            String description,
            List<String> channelValues,
            DeliveryDestination destination
    ) {
        String normalizedOperationId = validateOperationId(operationId);
        String normalizedDescription = normalizeDescription(description);
        List<OtpChannel> channels = parseChannels(channelValues);

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

        otpSender.send(channels, new OtpMessage(
                userId,
                normalizedOperationId,
                plainCode,
                destination == null ? null : destination.getPhone(),
                destination == null ? null : destination.getEmail(),
                destination == null ? null : destination.getTelegramChatId()
        ));

        logger.info("OTP generated userId={} operationId={} otpId={} channels={} expiresAt={}",
                userId,
                normalizedOperationId,
                savedOtpCode.id(),
                channels,
                savedOtpCode.expiresAt()
        );

        return savedOtpCode;
    }

    public OtpCode validate(UUID userId, String operationId, String code) {
        String normalizedOperationId = validateOperationId(operationId);
        String normalizedCode = validateCode(code);

        Operation operation = operationDao
                .findByUserIdAndOperationId(userId, normalizedOperationId)
                .orElseThrow(() -> new NotFoundException("Active OTP code not found"));

        OtpCode otpCode = otpCodeDao
                .findActiveByUserIdAndOperationId(userId, operation.id())
                .orElseThrow(() -> new NotFoundException("Active OTP code not found"));

        Instant now = Instant.now();

        if (otpCode.expiresAt().isBefore(now) || otpCode.expiresAt().equals(now)) {
            otpCodeDao.markExpired(otpCode.id());
            throw new BadRequestException("OTP code expired");
        }

        if (!passwordHasher.verify(normalizedCode, otpCode.codeHash())) {
            throw new BadRequestException("Invalid OTP code");
        }

        Instant usedAt = Instant.now();
        otpCodeDao.markUsed(otpCode.id(), usedAt);

        logger.info("OTP validated successfully userId={} operationId={} otpId={}",
                userId,
                normalizedOperationId,
                otpCode.id()
        );

        return new OtpCode(
                otpCode.id(),
                otpCode.userId(),
                otpCode.operationId(),
                otpCode.codeHash(),
                OtpStatus.USED,
                otpCode.expiresAt(),
                otpCode.createdAt(),
                usedAt
        );
    }

    private List<OtpChannel> parseChannels(List<String> channelValues) {
        if (channelValues == null || channelValues.isEmpty()) {
            return List.of(OtpChannel.FILE);
        }

        return channelValues.stream()
                .map(this::parseChannel)
                .distinct()
                .toList();
    }

    private OtpChannel parseChannel(String value) {
        if (value == null || value.isBlank()) {
            throw new BadRequestException("OTP channel is required");
        }

        try {
            return OtpChannel.valueOf(value.trim().toUpperCase());
        } catch (IllegalArgumentException exception) {
            throw new BadRequestException("Unsupported OTP channel: " + value);
        }
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

    private String validateCode(String code) {
        if (code == null || code.isBlank()) {
            throw new BadRequestException("OTP code is required");
        }

        String normalizedCode = code.trim();

        if (!normalizedCode.matches("\\d{4,10}")) {
            throw new BadRequestException("OTP code must contain from 4 to 10 digits");
        }

        return normalizedCode;
    }

    private String normalizeDescription(String description) {
        if (description == null || description.isBlank()) {
            return null;
        }

        return description.trim();
    }
}