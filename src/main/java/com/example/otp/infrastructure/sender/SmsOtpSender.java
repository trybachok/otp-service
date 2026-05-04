package com.example.otp.infrastructure.sender;

import com.example.otp.application.port.OtpMessage;
import com.example.otp.application.port.OtpSender;
import com.example.otp.domain.exception.BadRequestException;
import com.example.otp.domain.model.OtpChannel;
import org.jsmpp.bean.BindType;
import org.jsmpp.bean.ESMClass;
import org.jsmpp.bean.GeneralDataCoding;
import org.jsmpp.bean.NumberingPlanIndicator;
import org.jsmpp.bean.RegisteredDelivery;
import org.jsmpp.bean.SMSCDeliveryReceipt;
import org.jsmpp.bean.TypeOfNumber;
import org.jsmpp.session.BindParameter;
import org.jsmpp.session.SMPPSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.charset.StandardCharsets;
import java.util.Properties;

public final class SmsOtpSender implements OtpSender {

    private static final Logger logger = LoggerFactory.getLogger(SmsOtpSender.class);

    private final Properties config;

    public SmsOtpSender() {
        this.config = loadConfig();
    }

    @Override
    public OtpChannel channel() {
        return OtpChannel.SMS;
    }

    @Override
    public void send(OtpMessage message) {
        if (message.phone() == null || message.phone().isBlank()) {
            throw new BadRequestException("Phone is required for SMS channel");
        }

        boolean enabled = Boolean.parseBoolean(config.getProperty("sms.enabled", "false"));

        if (!enabled) {
            logger.info("SMS emulator disabled. Simulated SMS to={} operationId={} code={}",
                    message.phone(),
                    message.operationId(),
                    message.code()
            );
            return;
        }

        SMPPSession session = new SMPPSession();

        try {
            String host = config.getProperty("smpp.host");
            int port = Integer.parseInt(config.getProperty("smpp.port"));
            String systemId = config.getProperty("smpp.system_id");
            String password = config.getProperty("smpp.password");
            String systemType = config.getProperty("smpp.system_type");
            String sourceAddr = config.getProperty("smpp.source_addr");

            BindParameter bindParameter = new BindParameter(
                    BindType.BIND_TX,
                    systemId,
                    password,
                    systemType,
                    TypeOfNumber.UNKNOWN,
                    NumberingPlanIndicator.UNKNOWN,
                    sourceAddr
            );

            session.connectAndBind(host, port, bindParameter);

            session.submitShortMessage(
                    systemType,
                    TypeOfNumber.UNKNOWN,
                    NumberingPlanIndicator.UNKNOWN,
                    sourceAddr,
                    TypeOfNumber.UNKNOWN,
                    NumberingPlanIndicator.UNKNOWN,
                    message.phone(),
                    new ESMClass(),
                    (byte) 0,
                    (byte) 1,
                    null,
                    null,
                    new RegisteredDelivery(SMSCDeliveryReceipt.DEFAULT),
                    (byte) 0,
                    new GeneralDataCoding(),
                    (byte) 0,
                    ("Your OTP code is: " + message.code()).getBytes(StandardCharsets.UTF_8)
            );

            logger.info("SMS sent through SMPP userId={} phone={} operationId={}",
                    message.userId(),
                    message.phone(),
                    message.operationId()
            );
        } catch (Exception exception) {
            logger.error("Failed to send SMS userId={} phone={} operationId={}",
                    message.userId(),
                    message.phone(),
                    message.operationId(),
                    exception
            );

            throw new IllegalStateException("Failed to send SMS", exception);
        } finally {
            session.unbindAndClose();
        }
    }

    private Properties loadConfig() {
        return SenderConfig.load(
                "sms.properties",
                new SenderConfig.EnvironmentOverride("sms.enabled", "SMS_ENABLED"),
                new SenderConfig.EnvironmentOverride("smpp.host", "SMPP_HOST"),
                new SenderConfig.EnvironmentOverride("smpp.port", "SMPP_PORT"),
                new SenderConfig.EnvironmentOverride("smpp.system_id", "SMPP_SYSTEM_ID"),
                new SenderConfig.EnvironmentOverride("smpp.password", "SMPP_PASSWORD"),
                new SenderConfig.EnvironmentOverride("smpp.system_type", "SMPP_SYSTEM_TYPE"),
                new SenderConfig.EnvironmentOverride("smpp.source_addr", "SMPP_SOURCE_ADDR")
        );
    }
}
