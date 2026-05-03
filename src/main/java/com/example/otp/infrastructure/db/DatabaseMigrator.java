package com.example.otp.infrastructure.db;

import com.example.otp.config.AppConfig;
import org.flywaydb.core.Flyway;

public final class DatabaseMigrator {

    private final AppConfig appConfig;

    public DatabaseMigrator(AppConfig appConfig) {
        this.appConfig = appConfig;
    }

    public void migrate() {
        Flyway flyway = Flyway.configure()
                .dataSource(
                        appConfig.databaseUrl(),
                        appConfig.databaseUsername(),
                        appConfig.databasePassword()
                )
                .locations(appConfig.flywayLocations())
                .load();

        flyway.migrate();
    }
}