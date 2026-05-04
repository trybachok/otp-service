package com.example.otp.infrastructure.sender;

import com.example.otp.application.port.OtpMessage;
import com.example.otp.application.port.OtpSender;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.Instant;

public final class FileOtpSender implements OtpSender {

    private static final Logger logger = LoggerFactory.getLogger(FileOtpSender.class);

    private final Path filePath;

    public FileOtpSender(Path filePath) {
        this.filePath = filePath;
    }

    @Override
    public void send(OtpMessage message) {
        String line = "%s userId=%s operationId=%s code=%s%n".formatted(
                Instant.now(),
                message.userId(),
                message.operationId(),
                message.code()
        );

        try {
            Files.writeString(
                    filePath,
                    line,
                    StandardOpenOption.CREATE,
                    StandardOpenOption.APPEND
            );

            logger.info("OTP code saved to file userId={} operationId={} file={}",
                    message.userId(),
                    message.operationId(),
                    filePath
            );
        } catch (IOException exception) {
            logger.error("Failed to save OTP code to file userId={} operationId={}",
                    message.userId(),
                    message.operationId(),
                    exception
            );

            throw new IllegalStateException("Failed to save OTP code to file", exception);
        }
    }
}