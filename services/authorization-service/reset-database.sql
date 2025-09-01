-- Reset script for authorization service database
-- Run this to clean up the database state when Flyway migrations are in conflict

-- Drop all tables if they exist (in dependency order)
DROP TABLE IF EXISTS field_access CASCADE;
DROP TABLE IF EXISTS role_permissions CASCADE;
DROP TABLE IF EXISTS user_roles CASCADE;
DROP TABLE IF EXISTS api_methods CASCADE;
DROP TABLE IF EXISTS roles CASCADE;

-- Drop Flyway schema history table to reset migration state
DROP TABLE IF EXISTS flyway_schema_history CASCADE;

-- The application will recreate all tables with proper Flyway migration on next startup
