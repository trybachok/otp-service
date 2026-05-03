package com.example.otp.application.service;

import com.example.otp.domain.exception.BadRequestException;
import com.example.otp.domain.model.OtpConfig;
import com.example.otp.infrastructure.dao.OtpConfigDao;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class OtpConfigServiceTest {

    private OtpConfigDao otpConfigDao;
    private OtpConfigService service;

    @BeforeEach
    void setUp() {
        otpConfigDao = mock(OtpConfigDao.class);
        service = new OtpConfigService(otpConfigDao);
    }

    @Test
    void updateConfigUpdatesValidConfig() {
        OtpConfig config = new OtpConfig(1, 6, 300, Instant.now());

        when(otpConfigDao.updateConfig(6, 300)).thenReturn(config);

        OtpConfig result = service.updateConfig(6, 300);

        assertEquals(6, result.codeLength());
        assertEquals(300, result.ttlSeconds());

        verify(otpConfigDao).updateConfig(6, 300);
    }

    @Test
    void updateConfigRejectsTooShortCodeLength() {
        BadRequestException exception = assertThrows(
                BadRequestException.class,
                () -> service.updateConfig(3, 300)
        );

        assertEquals("Code length must be between 4 and 10", exception.getMessage());

        verify(otpConfigDao, never()).updateConfig(anyInt(), anyInt());
    }

    @Test
    void updateConfigRejectsTooSmallTtl() {
        BadRequestException exception = assertThrows(
                BadRequestException.class,
                () -> service.updateConfig(6, 10)
        );

        assertEquals("TTL seconds must be between 30 and 86400", exception.getMessage());

        verify(otpConfigDao, never()).updateConfig(anyInt(), anyInt());
    }
}