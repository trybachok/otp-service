package com.example.otp.infrastructure.security;

import com.example.otp.application.port.OtpCodeGenerator;
import com.example.otp.domain.exception.BadRequestException;

import java.security.SecureRandom;

public final class SecureRandomOtpCodeGenerator implements OtpCodeGenerator {

    private static final int MIN_LENGTH = 4;
    private static final int MAX_LENGTH = 10;

    private final SecureRandom secureRandom = new SecureRandom();

    @Override
    public String generate(int length) {
        if (length < MIN_LENGTH || length > MAX_LENGTH) {
            throw new BadRequestException("OTP length must be between 4 and 10");
        }

        StringBuilder code = new StringBuilder(length);

        for (int index = 0; index < length; index++) {
            code.append(secureRandom.nextInt(10));
        }

        return code.toString();
    }
}