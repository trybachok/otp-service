package com.example.otp.application.service;

import com.example.otp.application.port.OtpCodeGenerator;
import com.example.otp.application.port.OtpMessage;
import com.example.otp.application.port.OtpSender;
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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
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
    private TestOtpSender testOtpSender;
    private OtpService otpService;

    @BeforeEach
    void setUp() {
        otpConfigDao = mock(OtpConfigDao.class);
        operationDao = mock(OperationDao.class);
        otpCodeDao = mock(OtpCodeDao.class);
        otpCodeGenerator = mock(OtpCodeGenerator.class);
        passwordHasher = mock(PasswordHasher.class);

        testOtpSender = new TestOtpSender();
        CompositeOtpSender compositeOtpSender = new CompositeOtpSender(List.of(testOtpSender));

        otpService = new OtpService(
                otpConfigDao,
                operationDao,
                otpCodeDao,
                otpCodeGenerator,
                passwordHasher,
                compositeOtpSender
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

        assertTrue(testOtpSender.wasCalled());
        assertEquals("123456", testOtpSender.lastMessage().code());
        assertEquals("payment-123", testOtpSender.lastMessage().operationId());

        verify(operationDao).save(any(Operation.class));
        verify(otpCodeDao).save(any(OtpCode.class));
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
        assertTrue(testOtpSender.wasCalled());

        verify(operationDao, never()).save(any(Operation.class));
        verify(otpCodeDao).save(any(OtpCode.class));
    }

    @Test
    void generateRejectsEmptyOperationId() {
        BadRequestException exception = assertThrows(
                BadRequestException.class,
                () -> otpService.generate(UUID.randomUUID(), "", "description")
        );

        assertEquals("Operation id is required", exception.getMessage());

        assertFalse(testOtpSender.wasCalled());
        verify(otpCodeDao, never()).save(any());
    }

    @Test
    void generateRejectsUnsupportedChannel() {
        UUID userId = UUID.randomUUID();

        BadRequestException exception = assertThrows(
                BadRequestException.class,
                () -> otpService.generate(
                        userId,
                        "payment-123",
                        "Confirm payment",
                        List.of("PIGEON"),
                        null
                )
        );

        assertEquals("Unsupported OTP channel: PIGEON", exception.getMessage());

        assertFalse(testOtpSender.wasCalled());
        verify(otpCodeDao, never()).save(any());
    }

    @Test
    void validateMarksOtpAsUsedWhenCodeIsCorrect() {
        UUID userId = UUID.randomUUID();
        UUID operationDbId = UUID.randomUUID();
        UUID otpId = UUID.randomUUID();

        Operation operation = new Operation(
                operationDbId,
                userId,
                "payment-123",
                "Confirm payment",
                Instant.now()
        );

        OtpCode otpCode = new OtpCode(
                otpId,
                userId,
                operationDbId,
                "hashed-code",
                OtpStatus.ACTIVE,
                Instant.now().plusSeconds(300),
                Instant.now(),
                null
        );

        when(operationDao.findByUserIdAndOperationId(userId, "payment-123")).thenReturn(Optional.of(operation));
        when(otpCodeDao.findActiveByUserIdAndOperationId(userId, operationDbId)).thenReturn(Optional.of(otpCode));
        when(passwordHasher.verify("123456", "hashed-code")).thenReturn(true);

        OtpCode result = otpService.validate(userId, "payment-123", "123456");

        assertEquals(OtpStatus.USED, result.status());
        assertNotNull(result.usedAt());

        verify(otpCodeDao).markUsed(eq(otpId), any(Instant.class));
    }

    @Test
    void validateRejectsUnknownOperation() {
        UUID userId = UUID.randomUUID();

        when(operationDao.findByUserIdAndOperationId(userId, "payment-123"))
                .thenReturn(Optional.empty());

        NotFoundException exception = assertThrows(
                NotFoundException.class,
                () -> otpService.validate(userId, "payment-123", "123456")
        );

        assertEquals("Active OTP code not found", exception.getMessage());

        verify(otpCodeDao, never()).markUsed(any(), any());
    }

    @Test
    void validateRejectsMissingActiveOtpCode() {
        UUID userId = UUID.randomUUID();
        UUID operationDbId = UUID.randomUUID();

        Operation operation = new Operation(
                operationDbId,
                userId,
                "payment-123",
                "Confirm payment",
                Instant.now()
        );

        when(operationDao.findByUserIdAndOperationId(userId, "payment-123")).thenReturn(Optional.of(operation));
        when(otpCodeDao.findActiveByUserIdAndOperationId(userId, operationDbId)).thenReturn(Optional.empty());

        NotFoundException exception = assertThrows(
                NotFoundException.class,
                () -> otpService.validate(userId, "payment-123", "123456")
        );

        assertEquals("Active OTP code not found", exception.getMessage());

        verify(otpCodeDao, never()).markUsed(any(), any());
    }

    @Test
    void validateRejectsExpiredOtpAndMarksExpired() {
        UUID userId = UUID.randomUUID();
        UUID operationDbId = UUID.randomUUID();
        UUID otpId = UUID.randomUUID();

        Operation operation = new Operation(
                operationDbId,
                userId,
                "payment-123",
                "Confirm payment",
                Instant.now()
        );

        OtpCode otpCode = new OtpCode(
                otpId,
                userId,
                operationDbId,
                "hashed-code",
                OtpStatus.ACTIVE,
                Instant.now().minusSeconds(1),
                Instant.now().minusSeconds(300),
                null
        );

        when(operationDao.findByUserIdAndOperationId(userId, "payment-123")).thenReturn(Optional.of(operation));
        when(otpCodeDao.findActiveByUserIdAndOperationId(userId, operationDbId)).thenReturn(Optional.of(otpCode));

        BadRequestException exception = assertThrows(
                BadRequestException.class,
                () -> otpService.validate(userId, "payment-123", "123456")
        );

        assertEquals("OTP code expired", exception.getMessage());

        verify(otpCodeDao).markExpired(otpId);
        verify(otpCodeDao, never()).markUsed(any(), any());
    }

    @Test
    void validateRejectsWrongCode() {
        UUID userId = UUID.randomUUID();
        UUID operationDbId = UUID.randomUUID();
        UUID otpId = UUID.randomUUID();

        Operation operation = new Operation(
                operationDbId,
                userId,
                "payment-123",
                "Confirm payment",
                Instant.now()
        );

        OtpCode otpCode = new OtpCode(
                otpId,
                userId,
                operationDbId,
                "hashed-code",
                OtpStatus.ACTIVE,
                Instant.now().plusSeconds(300),
                Instant.now(),
                null
        );

        when(operationDao.findByUserIdAndOperationId(userId, "payment-123")).thenReturn(Optional.of(operation));
        when(otpCodeDao.findActiveByUserIdAndOperationId(userId, operationDbId)).thenReturn(Optional.of(otpCode));
        when(passwordHasher.verify("000000", "hashed-code")).thenReturn(false);

        BadRequestException exception = assertThrows(
                BadRequestException.class,
                () -> otpService.validate(userId, "payment-123", "000000")
        );

        assertEquals("Invalid OTP code", exception.getMessage());

        verify(otpCodeDao, never()).markUsed(any(), any());
    }

    @Test
    void validateRejectsEmptyCode() {
        BadRequestException exception = assertThrows(
                BadRequestException.class,
                () -> otpService.validate(UUID.randomUUID(), "payment-123", "")
        );

        assertEquals("OTP code is required", exception.getMessage());
    }

    @Test
    void validateRejectsNonDigitCode() {
        BadRequestException exception = assertThrows(
                BadRequestException.class,
                () -> otpService.validate(UUID.randomUUID(), "payment-123", "abc123")
        );

        assertEquals("OTP code must contain from 4 to 10 digits", exception.getMessage());
    }

    private static final class TestOtpSender implements OtpSender {

        private OtpMessage lastMessage;

        @Override
        public OtpChannel channel() {
            return OtpChannel.FILE;
        }

        @Override
        public void send(OtpMessage message) {
            this.lastMessage = message;
        }

        boolean wasCalled() {
            return lastMessage != null;
        }

        OtpMessage lastMessage() {
            return lastMessage;
        }
    }
}