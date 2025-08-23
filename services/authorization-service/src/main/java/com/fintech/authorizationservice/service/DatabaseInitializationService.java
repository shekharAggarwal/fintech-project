package com.fintech.authorizationservice.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.stream.Collectors;

/**
 * Service to initialize database with required data when application starts
 */
@Service
public class DatabaseInitializationService {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @EventListener(ApplicationReadyEvent.class)
    @Transactional
    public void initializeDatabase() {
        try {
            System.out.println("Starting database initialization...");
            
            // Create migration tracking table
            createMigrationTable();
            
            // Execute data initialization scripts
            executeDataInitialization();
            
            System.out.println("Database initialization completed successfully");
        } catch (Exception e) {
            System.err.println("Database initialization failed: " + e.getMessage());
            e.printStackTrace();
            // Don't fail the application startup
        }
    }

    private void createMigrationTable() {
        try {
            String createTableSql = """
                CREATE TABLE IF NOT EXISTS schema_migrations (
                    version VARCHAR(50) PRIMARY KEY,
                    description VARCHAR(200),
                    executed_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
                )
                """;
            jdbcTemplate.execute(createTableSql);
        } catch (Exception e) {
            System.out.println("Migration table already exists or creation failed: " + e.getMessage());
        }
    }

    private void executeDataInitialization() {
        // Initialize roles if not exist
        initializeRoles();
        
        // Initialize API methods if not exist
        initializeApiMethods();
        
        // Initialize role permissions if not exist
        initializeRolePermissions();
        
        // Initialize field access permissions if not exist
        initializeFieldAccess();
    }

    private void initializeRoles() {
        try {
            // Check if roles already exist
            Integer roleCount = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM roles", Integer.class);
            if (roleCount != null && roleCount > 0) {
                System.out.println("Roles already initialized");
                return;
            }

            // Insert predefined roles
            String[] roles = {
                "('ADMIN', 'System administrator with full access to all resources and operations')",
                "('MANAGER', 'Branch/Department manager with elevated privileges for user and account management')",
                "('EMPLOYEE', 'Bank employee with access to customer service and basic operations')",
                "('ACCOUNT_HOLDER', 'Regular customer with access to their own account operations')",
                "('BUSINESS_ACCOUNT_HOLDER', 'Business customer with enhanced account features and transaction limits')",
                "('PREMIUM_ACCOUNT_HOLDER', 'Premium customer with additional services and higher transaction limits')",
                "('AUDITOR', 'Internal/External auditor with read-only access to transaction and account data')",
                "('COMPLIANCE_OFFICER', 'Compliance officer with access to monitoring and reporting features')",
                "('RISK_ANALYST', 'Risk management analyst with access to risk assessment and reporting tools')",
                "('CUSTOMER_SUPPORT', 'Customer support representative with limited account access for assistance')",
                "('TELLER', 'Bank teller with access to basic banking operations and cash handling')",
                "('LOAN_OFFICER', 'Loan officer with access to loan processing and approval workflows')",
                "('INVESTMENT_ADVISOR', 'Investment advisor with access to portfolio management and investment tools')",
                "('TREASURY_OFFICER', 'Treasury officer with access to liquidity management and treasury operations')",
                "('SECURITY_OFFICER', 'Security officer with access to security monitoring and incident management')"
            };

            StringBuilder insertSql = new StringBuilder("INSERT INTO roles (name, description) VALUES ");
            insertSql.append(String.join(", ", roles));

            jdbcTemplate.execute(insertSql.toString());
            System.out.println("Roles initialized successfully");
            
            recordMigration("roles_init", "Initialize predefined roles");
        } catch (Exception e) {
            System.err.println("Failed to initialize roles: " + e.getMessage());
        }
    }

    private void initializeApiMethods() {
        try {
            // Check if API methods already exist
            Integer apiMethodCount = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM api_methods", Integer.class);
            if (apiMethodCount != null && apiMethodCount > 0) {
                System.out.println("API methods already initialized");
                return;
            }

            // Execute the API methods initialization script
            executeScript("db/migration/V3__Insert_api_methods.sql");
            System.out.println("API methods initialized successfully");
            
            recordMigration("api_methods_init", "Initialize API methods");
        } catch (Exception e) {
            System.err.println("Failed to initialize API methods: " + e.getMessage());
        }
    }

    private void initializeRolePermissions() {
        try {
            // Check if role permissions already exist
            Integer permissionCount = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM role_permissions", Integer.class);
            if (permissionCount != null && permissionCount > 0) {
                System.out.println("Role permissions already initialized");
                return;
            }

            // Initialize basic permissions for each role
            initializeBasicPermissions();
            System.out.println("Role permissions initialized successfully");
            
            recordMigration("role_permissions_init", "Initialize role permissions");
        } catch (Exception e) {
            System.err.println("Failed to initialize role permissions: " + e.getMessage());
        }
    }

    private void initializeBasicPermissions() {
        // Admin gets all permissions
        jdbcTemplate.execute("""
            INSERT INTO role_permissions (role_id, method_id, allowed)
            SELECT r.role_id, am.api_method_id, true
            FROM roles r, api_methods am
            WHERE r.name = 'ADMIN'
            """);

        // Account holder gets basic permissions
        jdbcTemplate.execute("""
            INSERT INTO role_permissions (role_id, method_id, allowed, limit_type, limit_value)
            SELECT r.role_id, am.api_method_id, true, 'DAILY_AMOUNT', 50000
            FROM roles r, api_methods am
            WHERE r.name = 'ACCOUNT_HOLDER' 
            AND am.path IN ('/api/users/profile', '/api/payments/transfer', '/api/payments/history', '/api/transactions/history')
            """);

        // Employee gets customer service permissions
        jdbcTemplate.execute("""
            INSERT INTO role_permissions (role_id, method_id, allowed)
            SELECT r.role_id, am.api_method_id, true
            FROM roles r, api_methods am
            WHERE r.name = 'EMPLOYEE' 
            AND (am.path LIKE '/api/users/%' OR am.path LIKE '/api/payments/%status' OR am.path LIKE '/api/transactions/%')
            """);
    }

    private void initializeFieldAccess() {
        try {
            // Check if field access already exist
            Integer fieldAccessCount = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM field_access", Integer.class);
            if (fieldAccessCount != null && fieldAccessCount > 0) {
                System.out.println("Field access permissions already initialized");
                return;
            }

            // Initialize basic field access
            initializeBasicFieldAccess();
            System.out.println("Field access permissions initialized successfully");
            
            recordMigration("field_access_init", "Initialize field access permissions");
        } catch (Exception e) {
            System.err.println("Failed to initialize field access: " + e.getMessage());
        }
    }

    private void initializeBasicFieldAccess() {
        // Admin gets full access
        jdbcTemplate.execute("""
            INSERT INTO field_access (role_id, resource_type, allowed_fields, config)
            SELECT role_id, 'user', '["*"]', '{"access_level": "full", "can_modify": true}'
            FROM roles WHERE name = 'ADMIN'
            """);

        jdbcTemplate.execute("""
            INSERT INTO field_access (role_id, resource_type, allowed_fields, config)
            SELECT role_id, 'account', '["*"]', '{"access_level": "full", "can_modify": true}'
            FROM roles WHERE name = 'ADMIN'
            """);

        // Account holder gets limited access
        jdbcTemplate.execute("""
            INSERT INTO field_access (role_id, resource_type, allowed_fields, config)
            SELECT role_id, 'user', 
            '["id", "username", "email", "firstName", "lastName", "phoneNumber"]',
            '{"access_level": "self_only", "can_modify": ["email", "phoneNumber"]}'
            FROM roles WHERE name = 'ACCOUNT_HOLDER'
            """);

        jdbcTemplate.execute("""
            INSERT INTO field_access (role_id, resource_type, allowed_fields, config)
            SELECT role_id, 'account', 
            '["accountNumber", "accountType", "balance", "currency", "status"]',
            '{"access_level": "self_only", "can_modify": []}'
            FROM roles WHERE name = 'ACCOUNT_HOLDER'
            """);
    }

    private void executeScript(String scriptPath) throws IOException {
        ClassPathResource resource = new ClassPathResource(scriptPath);
        String sql;
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(resource.getInputStream(), StandardCharsets.UTF_8))) {
            sql = reader.lines()
                    .filter(line -> !line.trim().startsWith("--") && !line.trim().isEmpty())
                    .collect(Collectors.joining("\n"));
        }

        // Execute the SQL
        if (!sql.trim().isEmpty()) {
            jdbcTemplate.execute(sql);
        }
    }

    private void recordMigration(String version, String description) {
        try {
            jdbcTemplate.update(
                "INSERT INTO schema_migrations (version, description) VALUES (?, ?) ON CONFLICT (version) DO NOTHING",
                version, description);
        } catch (Exception e) {
            System.out.println("Failed to record migration: " + e.getMessage());
        }
    }
}
