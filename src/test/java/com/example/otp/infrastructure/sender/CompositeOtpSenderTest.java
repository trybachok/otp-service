package com.example.otp.infrastructure.sender;

import com.example.otp.application.port.OtpMessage;
import com.example.otp.application.port.OtpSender;
import com.example.otp.domain.model.OtpChannel;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.mockito.Mockito.*;

class CompositeOtpSenderTest {

    @Test
    void sendCallsOnlyRequestedChannels() {
        OtpSender fileSender = mock(OtpSender.class);
        OtpSender emailSender = mock(OtpSender.class);

        when(fileSender.channel()).thenReturn(OtpChannel.FILE);
        when(emailSender.channel()).thenReturn(OtpChannel.EMAIL);

        CompositeOtpSender compositeOtpSender = new CompositeOtpSender(List.of(
                fileSender,
                emailSender
        ));

        OtpMessage message = new OtpMessage(
                UUID.randomUUID(),
                "payment-123",
                "123456",
                null,
                "user@example.com",
                null
        );

        compositeOtpSender.send(List.of(OtpChannel.EMAIL), message);

        verify(emailSender).send(message);
        verify(fileSender, never()).send(any());
    }
}