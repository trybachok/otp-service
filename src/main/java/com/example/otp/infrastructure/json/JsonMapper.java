package com.example.otp.infrastructure.json;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

import java.io.IOException;
import java.io.InputStream;

public final class JsonMapper {

    private final ObjectMapper objectMapper;

    public JsonMapper() {
        this.objectMapper = new ObjectMapper();
        this.objectMapper.registerModule(new JavaTimeModule());
    }

    public <T> T read(InputStream inputStream, Class<T> targetClass) {
        try {
            return objectMapper.readValue(inputStream, targetClass);
        } catch (IOException exception) {
            throw new IllegalArgumentException("Invalid JSON request body", exception);
        }
    }

    public String write(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (IOException exception) {
            throw new IllegalStateException("Failed to serialize JSON response", exception);
        }
    }
}