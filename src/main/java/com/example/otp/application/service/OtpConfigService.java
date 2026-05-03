package com.example.otp.application.service;

import com.example.otp.domain.exception.BadRequestException;
import com.example.otp.domain.model.OtpConfig;
import com.example.otp.infrastructure.dao.OtpConfigDao;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class OtpConfigService {

    private static final Logger logger = LoggerFactory.getLogger(OtpConfigService.class);

    private final OtpConfigDao otpConfigDao;

    public OtpConfigService(OtpConfigDao otpConfigDao) {
        this.otpConfigDao = otpConfigDao;
    }

    public OtpConfig updateConfig(int codeLength, int ttlSeconds) {
        validateCodeLength(codeLength);
        validateTtlSeconds(ttlSeconds);

        OtpConfig updatedConfig = otpConfigDao.updateConfig(codeLength, ttlSeconds);

        logger.info("OTP config updated codeLength={} ttlSeconds={}",
                updatedConfig.codeLength(),
                updatedConfig.ttlSeconds()
        );

        return updatedConfig;
    }

    private void validateCodeLength(int codeLength) {
        if (codeLength < 4 || codeLength > 10) {
            throw new BadRequestException("Code length must be between 4 and 10");
        }
    }

    private void validateTtlSeconds(int ttlSeconds) {
        if (ttlSeconds < 30 || ttlSeconds > 86400) {
            throw new BadRequestException("TTL seconds must be between 30 and 86400");
        }
    }
}