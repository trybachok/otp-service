package com.example.otp.config;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;

public final class EnvironmentConfig {

    private static final Map<String, String> DOT_ENV = loadDotEnv();

    private EnvironmentConfig() {
    }

    public static String get(String name) {
        String dotEnvValue = DOT_ENV.get(name);

        if (dotEnvValue != null && !dotEnvValue.isBlank()) {
            return dotEnvValue;
        }

        String environmentValue = System.getenv(name);

        if (environmentValue != null && !environmentValue.isBlank()) {
            return environmentValue;
        }

        return null;
    }

    private static Map<String, String> loadDotEnv() {
        Path path = Path.of(".env");

        if (!Files.isRegularFile(path)) {
            return Map.of();
        }

        Properties properties = new Properties();

        try (var reader = Files.newBufferedReader(path)) {
            properties.load(reader);
        } catch (IOException exception) {
            throw new IllegalStateException("Failed to load .env", exception);
        }

        Map<String, String> values = new ConcurrentHashMap<>();

        for (String name : properties.stringPropertyNames()) {
            values.put(name, properties.getProperty(name));
        }

        return values;
    }
}
