package com.example.otp.infrastructure.sender;

import com.example.otp.config.EnvironmentConfig;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

final class SenderConfig {

    private SenderConfig() {
    }

    static Properties load(String resourceName, EnvironmentOverride... overrides) {
        Properties properties = new Properties();

        try (InputStream inputStream = SenderConfig.class
                .getClassLoader()
                .getResourceAsStream(resourceName)) {

            if (inputStream != null) {
                properties.load(inputStream);
            }

            for (EnvironmentOverride override : overrides) {
                String environmentValue = EnvironmentConfig.get(override.environmentName());

                if (environmentValue != null && !environmentValue.isBlank()) {
                    properties.setProperty(override.propertyName(), environmentValue);
                }
            }

            return properties;
        } catch (IOException exception) {
            throw new IllegalStateException("Failed to load sender configuration: " + resourceName, exception);
        }
    }

    record EnvironmentOverride(String propertyName, String environmentName) {
    }
}
