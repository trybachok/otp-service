package com.example.otp.infrastructure.security;

import com.example.otp.domain.exception.BadRequestException;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class SecureRandomOtpCodeGeneratorTest {

    private final SecureRandomOtpCodeGenerator generator = new SecureRandomOtpCodeGenerator();

    @Test
    void generateReturnsCodeWithRequestedLength() {
        String code = generator.generate(6);

        assertEquals(6, code.length());
    }

    @Test
    void generateReturnsOnlyDigits() {
        String code = generator.generate(10);

        assertTrue(code.matches("\\d{10}"));
    }

    @Test
    void generateRejectsTooShortLength() {
        BadRequestException exception = assertThrows(
                BadRequestException.class,
                () -> generator.generate(3)
        );

        assertEquals("OTP length must be between 4 and 10", exception.getMessage());
    }

    @Test
    void generateRejectsTooLongLength() {
        BadRequestException exception = assertThrows(
                BadRequestException.class,
                () -> generator.generate(11)
        );

        assertEquals("OTP length must be between 4 and 10", exception.getMessage());
    }
}