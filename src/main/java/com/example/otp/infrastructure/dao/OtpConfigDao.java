package com.example.otp.infrastructure.dao;

import com.example.otp.domain.model.OtpConfig;

public interface OtpConfigDao {

    OtpConfig getConfig();

    OtpConfig updateConfig(int codeLength, int ttlSeconds);
}