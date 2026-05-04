package com.example.otp.infrastructure.sender;

import com.example.otp.application.port.OtpMessage;
import com.example.otp.application.port.OtpSender;
import com.example.otp.domain.exception.BadRequestException;
import com.example.otp.domain.model.OtpChannel;
import jakarta.mail.Message;
import jakarta.mail.PasswordAuthentication;
import jakarta.mail.Session;
import jakarta.mail.Transport;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Properties;

public final class EmailOtpSender implements OtpSender {

    private static final Logger logger = LoggerFactory.getLogger(EmailOtpSender.class);

    private final Properties config;
    private final String username;
    private final String password;
    private final String from;
    private final Session session;

    public EmailOtpSender() {
        this.config = loadConfig();
        this.username = config.getProperty("email.username");
        this.password = config.getProperty("email.password");
        this.from = config.getProperty("email.from");

        this.session = Session.getInstance(config, new jakarta.mail.Authenticator() {
            @Override
            protected PasswordAuthentication getPasswordAuthentication() {
                return new PasswordAuthentication(username, password);
            }
        });
    }

    @Override
    public OtpChannel channel() {
        return OtpChannel.EMAIL;
    }

    @Override
    public void send(OtpMessage message) {
        if (message.email() == null || message.email().isBlank()) {
            throw new BadRequestException("Email is required for EMAIL channel");
        }

        boolean enabled = Boolean.parseBoolean(config.getProperty("email.enabled", "false"));

        if (!enabled) {
            logger.info("Email sending disabled. Simulated email to={} operationId={} code={}",
                    message.email(),
                    message.operationId(),
                    message.code()
            );
            return;
        }

        try {
            Message mailMessage = new MimeMessage(session);
            mailMessage.setFrom(new InternetAddress(from));
            mailMessage.setRecipients(Message.RecipientType.TO, InternetAddress.parse(message.email()));
            mailMessage.setSubject("OTP code");
            mailMessage.setText("Your OTP code is: " + message.code());

            Transport.send(mailMessage);

            logger.info("Email OTP sent userId={} email={} operationId={}",
                    message.userId(),
                    message.email(),
                    message.operationId()
            );
        } catch (Exception exception) {
            logger.error("Failed to send email OTP userId={} email={} operationId={}",
                    message.userId(),
                    message.email(),
                    message.operationId(),
                    exception
            );

            throw new IllegalStateException("Failed to send email", exception);
        }
    }

    private Properties loadConfig() {
        return SenderConfig.load(
                "email.properties",
                new SenderConfig.EnvironmentOverride("email.enabled", "EMAIL_ENABLED"),
                new SenderConfig.EnvironmentOverride("email.username", "EMAIL_USERNAME"),
                new SenderConfig.EnvironmentOverride("email.password", "EMAIL_PASSWORD"),
                new SenderConfig.EnvironmentOverride("email.from", "EMAIL_FROM"),
                new SenderConfig.EnvironmentOverride("mail.smtp.host", "MAIL_SMTP_HOST"),
                new SenderConfig.EnvironmentOverride("mail.smtp.port", "MAIL_SMTP_PORT"),
                new SenderConfig.EnvironmentOverride("mail.smtp.auth", "MAIL_SMTP_AUTH"),
                new SenderConfig.EnvironmentOverride("mail.smtp.starttls.enable", "MAIL_SMTP_STARTTLS_ENABLE")
        );
    }
}
