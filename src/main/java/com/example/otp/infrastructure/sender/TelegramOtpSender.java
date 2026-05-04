package com.example.otp.infrastructure.sender;

import com.example.otp.application.port.OtpMessage;
import com.example.otp.application.port.OtpSender;
import com.example.otp.domain.exception.BadRequestException;
import com.example.otp.domain.model.OtpChannel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.Properties;

public final class TelegramOtpSender implements OtpSender {

    private static final Logger logger = LoggerFactory.getLogger(TelegramOtpSender.class);

    private final Properties config;
    private final HttpClient httpClient;

    public TelegramOtpSender() {
        this.config = loadConfig();
        this.httpClient = HttpClient.newHttpClient();
    }

    @Override
    public OtpChannel channel() {
        return OtpChannel.TELEGRAM;
    }

    @Override
    public void send(OtpMessage message) {
        if (message.telegramChatId() == null || message.telegramChatId().isBlank()) {
            throw new BadRequestException("Telegram chat id is required for TELEGRAM channel");
        }

        boolean enabled = Boolean.parseBoolean(config.getProperty("telegram.enabled", "false"));

        if (!enabled) {
            logger.info("Telegram sending disabled. Simulated telegram chatId={} operationId={} code={}",
                    message.telegramChatId(),
                    message.operationId(),
                    message.code()
            );
            return;
        }

        try {
            String botToken = config.getProperty("telegram.bot_token");
            String text = "Your OTP code is: " + message.code();

            String url = "https://api.telegram.org/bot%s/sendMessage?chat_id=%s&text=%s".formatted(
                    botToken,
                    urlEncode(message.telegramChatId()),
                    urlEncode(text)
            );

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .GET()
                    .build();

            HttpResponse<String> response = httpClient.send(
                    request,
                    HttpResponse.BodyHandlers.ofString()
            );

            if (response.statusCode() >= 200 && response.statusCode() < 300) {
                logger.info("Telegram OTP sent userId={} chatId={} operationId={}",
                        message.userId(),
                        message.telegramChatId(),
                        message.operationId()
                );
            } else {
                logger.error("Telegram API error statusCode={} body={}",
                        response.statusCode(),
                        response.body()
                );

                throw new IllegalStateException("Telegram API error");
            }
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Telegram sending interrupted", exception);
        } catch (Exception exception) {
            logger.error("Failed to send Telegram OTP userId={} chatId={} operationId={}",
                    message.userId(),
                    message.telegramChatId(),
                    message.operationId(),
                    exception
            );

            throw new IllegalStateException("Failed to send Telegram message", exception);
        }
    }

    private Properties loadConfig() {
        try {
            Properties props = new Properties();
            props.load(TelegramOtpSender.class.getClassLoader().getResourceAsStream("telegram.properties"));
            return props;
        } catch (Exception exception) {
            throw new IllegalStateException("Failed to load Telegram configuration", exception);
        }
    }

    private String urlEncode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }
}