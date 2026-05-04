package com.example.otp.api.dto.otp;

import java.util.List;

public final class GenerateOtpRequest {

    private String operationId;
    private String description;
    private List<String> channels;
    private DeliveryDestination destination;

    public GenerateOtpRequest() {
    }

    public String getOperationId() {
        return operationId;
    }

    public String getDescription() {
        return description;
    }

    public List<String> getChannels() {
        return channels;
    }

    public DeliveryDestination getDestination() {
        return destination;
    }
}