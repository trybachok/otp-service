package com.example.otp.application.service;

import com.example.otp.application.port.OtpCodeGenerator;
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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class OtpServiceTest {

    private OtpConfigDao otpConfigDao;
    private OperationDao operationDao;
    private OtpCodeDao otpCodeDao;
    private OtpCodeGenerator otpCodeGenerator;
    private PasswordHasher passwordHasher;
    private OtpSender otpSender;
    private OtpService otpService;

    @BeforeEach
    void setUp() {
        otpConfigDao = mock(OtpConfigDao.class);
        operationDao = mock(OperationDao.class);
        otpCodeDao = mock(OtpCodeDao.class);
        otpCodeGenerator = mock(OtpCodeGenerator.class);
        passwordHasher = mock(PasswordHasher.class);
        otpSender = mock(OtpSender.class);

        otpService = new OtpService(
                otpConfigDao,
                operationDao,
                otpCodeDao,
                otpCodeGenerator,
                passwordHasher,
                otpSender
        );
    }

    @Test
    void generateCreatesOperationAndOtpCode() {
        UUID userId = UUID.randomUUID();

        when(otpConfigDao.getConfig()).thenReturn(new OtpConfig(1, 6, 300, Instant.now()));
        when(otpCodeGenerator.generate(6)).thenReturn("123456");
        when(passwordHasher.hash("123456")).thenReturn("hashed-code");
        when(operationDao.findByUserIdAndOperationId(userId, "payment-123")).thenReturn(Optional.empty());
        when(operationDao.save(any(Operation.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(otpCodeDao.save(any(OtpCode.class))).thenAnswer(invocation -> invocation.getArgument(0));

        OtpCode result = otpService.generate(userId, "payment-123", "Confirm payment");

        assertNotNull(result.id());
        assertEquals(userId, result.userId());
        assertEquals("hashed-code", result.codeHash());
        assertEquals(OtpStatus.ACTIVE, result.status());
        assertNotNull(result.expiresAt());

        verify(operationDao).save(any(Operation.class));
        verify(otpCodeDao).save(any(OtpCode.class));
        verify(otpSender).send(any());
    }

    @Test
    void generateUsesExistingOperationWhenExists() {
        UUID userId = UUID.randomUUID();
        Operation operation = new Operation(
                UUID.randomUUID(),
                userId,
                "payment-123",
                "Existing operation",
                Instant.now()
        );

        when(otpConfigDao.getConfig()).thenReturn(new OtpConfig(1, 6, 300, Instant.now()));
        when(otpCodeGenerator.generate(6)).thenReturn("123456");
        when(passwordHasher.hash("123456")).thenReturn("hashed-code");
        when(operationDao.findByUserIdAndOperationId(userId, "payment-123")).thenReturn(Optional.of(operation));
        when(otpCodeDao.save(any(OtpCode.class))).thenAnswer(invocation -> invocation.getArgument(0));

        OtpCode result = otpService.generate(userId, "payment-123", "Confirm payment");

        assertEquals(operation.id(), result.operationId());

        verify(operationDao, never()).save(any(Operation.class));
        verify(otpCodeDao).save(any(OtpCode.class));
        verify(otpSender).send(any());
    }

    @Test
    void generateRejectsEmptyOperationId() {
        BadRequestException exception = assertThrows(
                BadRequestException.class,
                () -> otpService.generate(UUID.randomUUID(), "", "description")
        );

        assertEquals("Operation id is required", exception.getMessage());

        verify(otpCodeDao, never()).save(any());
        verify(otpSender, never()).send(any());
    }
}