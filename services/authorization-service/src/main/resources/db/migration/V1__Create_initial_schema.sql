-- Initial schema creation for authorization service
-- This migration creates all the necessary tables for the authorization system

-- Create roles table
CREATE TABLE IF NOT EXISTS roles (
    role_id BIGSERIAL PRIMARY KEY,
    name VARCHAR(50) UNIQUE NOT NULL,
    description TEXT
);

-- Create api_methods table
CREATE TABLE IF NOT EXISTS api_methods (
    api_method_id BIGSERIAL PRIMARY KEY,
    path VARCHAR(255) UNIQUE NOT NULL,
    http_method VARCHAR(10) NOT NULL,
    description TEXT
);

-- Create role_permissions table
CREATE TABLE IF NOT EXISTS role_permissions (
    id BIGSERIAL PRIMARY KEY,
    role_id BIGINT NOT NULL REFERENCES roles(role_id) ON DELETE CASCADE,
    method_id BIGINT NOT NULL REFERENCES api_methods(api_method_id) ON DELETE CASCADE,
    allowed BOOLEAN NOT NULL DEFAULT false,
    limit_type VARCHAR(50),
    limit_value INTEGER,
    UNIQUE(role_id, method_id)
);

-- Create user_roles table
CREATE TABLE IF NOT EXISTS user_roles (
    id BIGSERIAL PRIMARY KEY,
    user_id VARCHAR(255) NOT NULL,
    role_id BIGINT NOT NULL REFERENCES roles(role_id) ON DELETE CASCADE,
    created_at BIGINT,
    UNIQUE(user_id, role_id)
);

-- Create field_access table
CREATE TABLE IF NOT EXISTS field_access (
    id BIGSERIAL PRIMARY KEY,
    role_id BIGINT NOT NULL REFERENCES roles(role_id) ON DELETE CASCADE,
    resource_type VARCHAR(100) NOT NULL,
    allowed_fields JSONB,
    config JSONB,
    UNIQUE(role_id, resource_type)
);

-- Create sessions table
CREATE TABLE IF NOT EXISTS sessions (
    id BIGSERIAL PRIMARY KEY,
    session_id VARCHAR(255) UNIQUE NOT NULL,
    user_id VARCHAR(255) NOT NULL,
    expiry_time BIGINT NOT NULL
);

-- Create indexes for better performance
CREATE INDEX IF NOT EXISTS idx_user_roles_user_id ON user_roles(user_id);
CREATE INDEX IF NOT EXISTS idx_user_roles_role_id ON user_roles(role_id);
CREATE INDEX IF NOT EXISTS idx_role_permissions_role_id ON role_permissions(role_id);
CREATE INDEX IF NOT EXISTS idx_role_permissions_method_id ON role_permissions(method_id);
CREATE INDEX IF NOT EXISTS idx_sessions_user_id ON sessions(user_id);
CREATE INDEX IF NOT EXISTS idx_sessions_session_id ON sessions(session_id);
CREATE INDEX IF NOT EXISTS idx_sessions_expiry_time ON sessions(expiry_time);
CREATE INDEX IF NOT EXISTS idx_field_access_role_id ON field_access(role_id);
CREATE INDEX IF NOT EXISTS idx_field_access_resource_type ON field_access(resource_type);
