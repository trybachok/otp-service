package com.example.otp.infrastructure.db;

import com.example.otp.config.AppConfig;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public final class ConnectionFactory {

    private final AppConfig appConfig;

    public ConnectionFactory(AppConfig appConfig) {
        this.appConfig = appConfig;
    }

    public Connection createConnection() throws SQLException {
        return DriverManager.getConnection(
                appConfig.databaseUrl(),
                appConfig.databaseUsername(),
                appConfig.databasePassword()
        );
    }
}