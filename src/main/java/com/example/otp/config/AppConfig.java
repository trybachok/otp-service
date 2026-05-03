package com.example.otp.config;

public final class AppConfig {

    private final int serverPort;

    private AppConfig(int serverPort) {
        this.serverPort = serverPort;
    }

    public static AppConfig load() {
        String portValue = System.getenv().getOrDefault("SERVER_PORT", "8080");
        int port = parsePort(portValue);

        return new AppConfig(port);
    }

    public int serverPort() {
        return serverPort;
    }

    private static int parsePort(String portValue) {
        try {
            int port = Integer.parseInt(portValue);
            if (port < 1 || port > 65535) {
                throw new IllegalArgumentException("SERVER_PORT must be between 1 and 65535, but was: " + portValue);
            }
            return port;
        } catch (NumberFormatException exception) {
            throw new IllegalArgumentException("SERVER_PORT must be a valid integer, but was: " + portValue, exception);
        }
    }
}
