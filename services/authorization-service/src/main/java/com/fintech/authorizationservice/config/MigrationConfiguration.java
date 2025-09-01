package com.fintech.authorizationservice.config;

import jakarta.annotation.PostConstruct;
import org.flywaydb.core.Flyway;
import org.springframework.context.annotation.Configuration;

import javax.sql.DataSource;

@Configuration
public class MigrationConfiguration {

    final private DataSource dataSource;

    public MigrationConfiguration(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    @PostConstruct
    public void migrate() {
        Flyway flyway = Flyway.configure()
                .dataSource(dataSource)
                .baselineOnMigrate(true)  // Key setting
                .baselineVersion("0")
                .locations("classpath:db/migration")
                .load();
        flyway.migrate();
    }
}