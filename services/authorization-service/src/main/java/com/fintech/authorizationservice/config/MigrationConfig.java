package com.fintech.authorizationservice.config;

import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

import javax.sql.DataSource;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.stream.Collectors;

/**
 * Configuration class for database migrations
 * This ensures migrations are executed when the application starts
 */
@Configuration
public class MigrationConfig {

    /**
     * CommandLineRunner to ensure migrations run at startup
     */
    @Bean
    @Profile("!test") // Don't run in test profile
    public CommandLineRunner migrationRunner(DataSource dataSource) {
        return args -> {
            try {
                JdbcTemplate jdbcTemplate = new JdbcTemplate(dataSource);
                executeMigrations(jdbcTemplate);
                System.out.println("Database migration completed successfully");
            } catch (Exception e) {
                System.err.println("Database migration failed: " + e.getMessage());
                e.printStackTrace();
                // Don't throw exception to allow application to start
                // throw new RuntimeException("Failed to execute database migrations", e);
            }
        };
    }

    @Transactional
    private void executeMigrations(JdbcTemplate jdbcTemplate) throws IOException {
        // Create migration tracking table if it doesn't exist
        createMigrationTable(jdbcTemplate);
        
        // Execute migrations in order
        String[] migrationFiles = {
            "V1__Create_initial_schema.sql",
            "V2__Insert_predefined_roles.sql", 
            "V3__Insert_api_methods.sql",
            "V4__Setup_role_permissions.sql",
            "V5__Setup_field_access_permissions.sql"
        };
        
        for (String migrationFile : migrationFiles) {
            if (!isMigrationExecuted(jdbcTemplate, migrationFile)) {
                System.out.println("Executing migration: " + migrationFile);
                executeMigrationFile(jdbcTemplate, migrationFile);
                markMigrationAsExecuted(jdbcTemplate, migrationFile);
            } else {
                System.out.println("Migration already executed: " + migrationFile);
            }
        }
    }
    
    private void createMigrationTable(JdbcTemplate jdbcTemplate) {
        String createTableSql = """
            CREATE TABLE IF NOT EXISTS flyway_schema_history (
                version VARCHAR(50) PRIMARY KEY,
                description VARCHAR(200),
                script VARCHAR(1000),
                executed_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                success BOOLEAN DEFAULT true
            )
            """;
        jdbcTemplate.execute(createTableSql);
    }
    
    private boolean isMigrationExecuted(JdbcTemplate jdbcTemplate, String migrationFile) {
        String version = extractVersionFromFilename(migrationFile);
        try {
            Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM flyway_schema_history WHERE version = ? AND success = true", 
                Integer.class, version);
            return count != null && count > 0;
        } catch (Exception e) {
            return false;
        }
    }
    
    private void executeMigrationFile(JdbcTemplate jdbcTemplate, String migrationFile) throws IOException {
        ClassPathResource resource = new ClassPathResource("db/migration/" + migrationFile);
        String sql;
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(resource.getInputStream(), StandardCharsets.UTF_8))) {
            sql = reader.lines().collect(Collectors.joining("\n"));
        }
        
        // Execute the SQL - split by semicolon for multiple statements
        String[] statements = sql.split(";");
        for (String statement : statements) {
            statement = statement.trim();
            if (!statement.isEmpty() && !statement.startsWith("--")) {
                jdbcTemplate.execute(statement);
            }
        }
    }
    
    private void markMigrationAsExecuted(JdbcTemplate jdbcTemplate, String migrationFile) {
        String version = extractVersionFromFilename(migrationFile);
        String description = extractDescriptionFromFilename(migrationFile);
        
        jdbcTemplate.update(
            "INSERT INTO flyway_schema_history (version, description, script, executed_at, success) VALUES (?, ?, ?, CURRENT_TIMESTAMP, true)",
            version, description, migrationFile);
    }
    
    private String extractVersionFromFilename(String filename) {
        // Extract version from filename like "V1__Create_initial_schema.sql" -> "1"
        return filename.substring(1, filename.indexOf("__"));
    }
    
    private String extractDescriptionFromFilename(String filename) {
        // Extract description from filename like "V1__Create_initial_schema.sql" -> "Create initial schema"
        String withoutVersion = filename.substring(filename.indexOf("__") + 2);
        String withoutExtension = withoutVersion.substring(0, withoutVersion.lastIndexOf("."));
        return withoutExtension.replace("_", " ");
    }
}
