package com.example.otp.application.scheduler;

import com.example.otp.infrastructure.dao.OtpCodeDao;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class OtpExpirationSchedulerTest {

    @Test
    void expireNowMarksExpiredCodes() {
        OtpCodeDao otpCodeDao = mock(OtpCodeDao.class);

        when(otpCodeDao.markExpiredCodes(any(Instant.class))).thenReturn(3);

        OtpExpirationScheduler scheduler = new OtpExpirationScheduler(otpCodeDao, 60);

        int expiredCount = scheduler.expireNow();

        assertEquals(3, expiredCount);

        verify(otpCodeDao).markExpiredCodes(any(Instant.class));
    }

    @Test
    void constructorRejectsZeroInterval() {
        OtpCodeDao otpCodeDao = mock(OtpCodeDao.class);

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> new OtpExpirationScheduler(otpCodeDao, 0)
        );

        assertEquals("Scheduler interval must be positive", exception.getMessage());
    }

    @Test
    void constructorRejectsNegativeInterval() {
        OtpCodeDao otpCodeDao = mock(OtpCodeDao.class);

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> new OtpExpirationScheduler(otpCodeDao, -1)
        );

        assertEquals("Scheduler interval must be positive", exception.getMessage());
    }
}