package com.example.otp.application.scheduler;

import com.example.otp.infrastructure.dao.OtpCodeDao;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public final class OtpExpirationScheduler {

    private static final Logger logger = LoggerFactory.getLogger(OtpExpirationScheduler.class);

    private final OtpCodeDao otpCodeDao;
    private final long intervalSeconds;
    private final ScheduledExecutorService executorService;

    public OtpExpirationScheduler(OtpCodeDao otpCodeDao, long intervalSeconds) {
        if (intervalSeconds <= 0) {
            throw new IllegalArgumentException("Scheduler interval must be positive");
        }

        this.otpCodeDao = otpCodeDao;
        this.intervalSeconds = intervalSeconds;
        this.executorService = Executors.newSingleThreadScheduledExecutor(runnable -> {
            Thread thread = new Thread(runnable);
            thread.setName("otp-expiration-scheduler");
            thread.setDaemon(true);
            return thread;
        });
    }

    public void start() {
        logger.info("OTP expiration scheduler started intervalSeconds={}", intervalSeconds);

        executorService.scheduleAtFixedRate(
                this::safeExpireCodes,
                intervalSeconds,
                intervalSeconds,
                TimeUnit.SECONDS
        );
    }

    public void stop() {
        logger.info("OTP expiration scheduler stopping");

        executorService.shutdown();

        try {
            if (!executorService.awaitTermination(5, TimeUnit.SECONDS)) {
                executorService.shutdownNow();
            }
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            executorService.shutdownNow();
        }

        logger.info("OTP expiration scheduler stopped");
    }

    public int expireNow() {
        Instant now = Instant.now();
        int expiredCount = otpCodeDao.markExpiredCodes(now);

        if (expiredCount > 0) {
            logger.info("Expired OTP codes marked count={}", expiredCount);
        } else {
            logger.debug("No expired OTP codes found");
        }

        return expiredCount;
    }

    private void safeExpireCodes() {
        try {
            expireNow();
        } catch (Exception exception) {
            logger.error("Failed to mark expired OTP codes", exception);
        }
    }
}