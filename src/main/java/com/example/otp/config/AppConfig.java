package com.example.otp.config;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

public final class AppConfig {

    private final int serverPort;
    private final String databaseUrl;
    private final String databaseUsername;
    private final String databasePassword;
    private final String flywayLocations;
    private final String tokenSecret;
    private final long tokenTtlSeconds;
    private final long otpExpirationSchedulerIntervalSeconds;

    private AppConfig(
            int serverPort,
            String databaseUrl,
            String databaseUsername,
            String databasePassword,
            String flywayLocations,
            String tokenSecret,
            long tokenTtlSeconds,
            long otpExpirationSchedulerIntervalSeconds
    ) {
        this.serverPort = serverPort;
        this.databaseUrl = databaseUrl;
        this.databaseUsername = databaseUsername;
        this.databasePassword = databasePassword;
        this.flywayLocations = flywayLocations;
        this.tokenSecret = tokenSecret;
        this.tokenTtlSeconds = tokenTtlSeconds;
        this.otpExpirationSchedulerIntervalSeconds = otpExpirationSchedulerIntervalSeconds;
    }

    public static AppConfig load() {
        Properties properties = loadProperties();

        int serverPort = Integer.parseInt(
                getValue(properties, "server.port", "SERVER_PORT", "8082")
        );

        String databaseUrl = getValue(properties, "db.url", "DB_URL", null);
        String databaseUsername = getValue(properties, "db.username", "DB_USERNAME", null);
        String databasePassword = getValue(properties, "db.password", "DB_PASSWORD", null);
        String flywayLocations = getValue(properties, "flyway.locations", "FLYWAY_LOCATIONS", "classpath:db/migration");
        String tokenSecret = getValue(properties, "token.secret", "TOKEN_SECRET", null);

        long tokenTtlSeconds = Long.parseLong(
                getValue(properties, "token.ttl.seconds", "TOKEN_TTL_SECONDS", "3600")
        );

        long otpExpirationSchedulerIntervalSeconds = Long.parseLong(
                getValue(
                        properties,
                        "otp.expiration.scheduler.interval.seconds",
                        "OTP_EXPIRATION_SCHEDULER_INTERVAL_SECONDS",
                        "60"
                )
        );

        return new AppConfig(
                serverPort,
                databaseUrl,
                databaseUsername,
                databasePassword,
                flywayLocations,
                tokenSecret,
                tokenTtlSeconds,
                otpExpirationSchedulerIntervalSeconds
        );
    }

    public int serverPort() {
        return serverPort;
    }

    public String databaseUrl() {
        return databaseUrl;
    }

    public String databaseUsername() {
        return databaseUsername;
    }

    public String databasePassword() {
        return databasePassword;
    }

    public String flywayLocations() {
        return flywayLocations;
    }

    public String tokenSecret() {
        return tokenSecret;
    }

    public long tokenTtlSeconds() {
        return tokenTtlSeconds;
    }

    public long otpExpirationSchedulerIntervalSeconds() {
        return otpExpirationSchedulerIntervalSeconds;
    }

    private static Properties loadProperties() {
        Properties properties = new Properties();

        try (InputStream inputStream = AppConfig.class
                .getClassLoader()
                .getResourceAsStream("application.properties")) {

            if (inputStream != null) {
                properties.load(inputStream);
            }

            return properties;
        } catch (IOException exception) {
            throw new IllegalStateException("Failed to load application.properties", exception);
        }
    }

    private static String getValue(
            Properties properties,
            String propertyName,
            String environmentName,
            String defaultValue
    ) {
        String environmentValue = System.getenv(environmentName);

        if (environmentValue != null && !environmentValue.isBlank()) {
            return environmentValue;
        }

        String propertyValue = properties.getProperty(propertyName);

        if (propertyValue != null && !propertyValue.isBlank()) {
            return propertyValue;
        }

        if (defaultValue != null) {
            return defaultValue;
        }

        throw new IllegalStateException("Required configuration value is missing: " + propertyName);
    }
}