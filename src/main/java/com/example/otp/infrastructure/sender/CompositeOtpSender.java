package com.example.otp.infrastructure.sender;

import com.example.otp.application.port.OtpMessage;
import com.example.otp.application.port.OtpSender;
import com.example.otp.domain.exception.BadRequestException;
import com.example.otp.domain.model.OtpChannel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

public final class CompositeOtpSender {

    private static final Logger logger = LoggerFactory.getLogger(CompositeOtpSender.class);

    private final Map<OtpChannel, OtpSender> senders = new EnumMap<>(OtpChannel.class);

    public CompositeOtpSender(List<OtpSender> senderList) {
        for (OtpSender sender : senderList) {
            senders.put(sender.channel(), sender);
        }
    }

    public void send(List<OtpChannel> channels, OtpMessage message) {
        for (OtpChannel channel : channels) {
            OtpSender sender = senders.get(channel);

            if (sender == null) {
                throw new BadRequestException("Unsupported OTP channel: " + channel);
            }

            logger.info("Sending OTP using channel={} userId={} operationId={}",
                    channel,
                    message.userId(),
                    message.operationId()
            );

            sender.send(message);
        }
    }
}