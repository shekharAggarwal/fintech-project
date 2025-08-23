-- Set up role permissions mapping
-- This migration assigns appropriate permissions to each role

-- Helper function to get role ID by name
CREATE OR REPLACE FUNCTION get_role_id(role_name VARCHAR) RETURNS BIGINT AS $$
DECLARE
    result BIGINT;
BEGIN
    SELECT role_id INTO result FROM roles WHERE name = role_name;
    RETURN result;
END;
$$ LANGUAGE plpgsql;

-- Helper function to get API method ID by path and method
CREATE OR REPLACE FUNCTION get_api_method_id(api_path VARCHAR, api_method VARCHAR) RETURNS BIGINT AS $$
DECLARE
    result BIGINT;
BEGIN
    SELECT api_method_id INTO result FROM api_methods WHERE path = api_path AND http_method = api_method;
    RETURN result;
END;
$$ LANGUAGE plpgsql;

-- ADMIN role permissions (full access)
INSERT INTO role_permissions (role_id, method_id, allowed, limit_type, limit_value)
SELECT get_role_id('ADMIN'), api_method_id, true, NULL, NULL
FROM api_methods
ON CONFLICT (role_id, method_id) DO NOTHING;

-- ACCOUNT_HOLDER permissions (basic user operations)
INSERT INTO role_permissions (role_id, method_id, allowed, limit_type, limit_value) VALUES
    (get_role_id('ACCOUNT_HOLDER'), get_api_method_id('/api/users/profile', 'GET'), true, NULL, NULL),
    (get_role_id('ACCOUNT_HOLDER'), get_api_method_id('/api/users/profile', 'PUT'), true, NULL, NULL),
    (get_role_id('ACCOUNT_HOLDER'), get_api_method_id('/api/payments/transfer', 'POST'), true, 'DAILY_AMOUNT', 50000),
    (get_role_id('ACCOUNT_HOLDER'), get_api_method_id('/api/payments/deposit', 'POST'), true, 'DAILY_AMOUNT', 100000),
    (get_role_id('ACCOUNT_HOLDER'), get_api_method_id('/api/payments/withdraw', 'POST'), true, 'DAILY_AMOUNT', 25000),
    (get_role_id('ACCOUNT_HOLDER'), get_api_method_id('/api/payments/history', 'GET'), true, 'RECORDS_PER_REQUEST', 100),
    (get_role_id('ACCOUNT_HOLDER'), get_api_method_id('/api/payments/*/status', 'GET'), true, NULL, NULL),
    (get_role_id('ACCOUNT_HOLDER'), get_api_method_id('/api/payments/*/cancel', 'POST'), true, NULL, NULL),
    (get_role_id('ACCOUNT_HOLDER'), get_api_method_id('/api/transactions/history', 'GET'), true, 'RECORDS_PER_REQUEST', 100),
    (get_role_id('ACCOUNT_HOLDER'), get_api_method_id('/api/transactions/*/details', 'GET'), true, NULL, NULL),
    (get_role_id('ACCOUNT_HOLDER'), get_api_method_id('/api/transactions/*/dispute', 'POST'), true, 'MONTHLY_COUNT', 5),
    (get_role_id('ACCOUNT_HOLDER'), get_api_method_id('/api/auth/logout', 'POST'), true, NULL, NULL),
    (get_role_id('ACCOUNT_HOLDER'), get_api_method_id('/api/auth/refresh', 'POST'), true, NULL, NULL),
    (get_role_id('ACCOUNT_HOLDER'), get_api_method_id('/api/auth/permissions', 'GET'), true, NULL, NULL),
    (get_role_id('ACCOUNT_HOLDER'), get_api_method_id('/api/notifications/history', 'GET'), true, 'RECORDS_PER_REQUEST', 50),
    (get_role_id('ACCOUNT_HOLDER'), get_api_method_id('/api/notifications/preferences', 'GET'), true, NULL, NULL),
    (get_role_id('ACCOUNT_HOLDER'), get_api_method_id('/api/notifications/preferences', 'PUT'), true, NULL, NULL),
    (get_role_id('ACCOUNT_HOLDER'), get_api_method_id('/api/reports/account-summary', 'GET'), true, NULL, NULL)
ON CONFLICT (role_id, method_id) DO NOTHING;

-- BUSINESS_ACCOUNT_HOLDER permissions (enhanced business operations)
INSERT INTO role_permissions (role_id, method_id, allowed, limit_type, limit_value) VALUES
    (get_role_id('BUSINESS_ACCOUNT_HOLDER'), get_api_method_id('/api/users/profile', 'GET'), true, NULL, NULL),
    (get_role_id('BUSINESS_ACCOUNT_HOLDER'), get_api_method_id('/api/users/profile', 'PUT'), true, NULL, NULL),
    (get_role_id('BUSINESS_ACCOUNT_HOLDER'), get_api_method_id('/api/payments/transfer', 'POST'), true, 'DAILY_AMOUNT', 500000),
    (get_role_id('BUSINESS_ACCOUNT_HOLDER'), get_api_method_id('/api/payments/deposit', 'POST'), true, 'DAILY_AMOUNT', 1000000),
    (get_role_id('BUSINESS_ACCOUNT_HOLDER'), get_api_method_id('/api/payments/withdraw', 'POST'), true, 'DAILY_AMOUNT', 200000),
    (get_role_id('BUSINESS_ACCOUNT_HOLDER'), get_api_method_id('/api/payments/bulk-transfer', 'POST'), true, 'DAILY_COUNT', 100),
    (get_role_id('BUSINESS_ACCOUNT_HOLDER'), get_api_method_id('/api/payments/history', 'GET'), true, 'RECORDS_PER_REQUEST', 500),
    (get_role_id('BUSINESS_ACCOUNT_HOLDER'), get_api_method_id('/api/payments/*/status', 'GET'), true, NULL, NULL),
    (get_role_id('BUSINESS_ACCOUNT_HOLDER'), get_api_method_id('/api/payments/*/cancel', 'POST'), true, NULL, NULL),
    (get_role_id('BUSINESS_ACCOUNT_HOLDER'), get_api_method_id('/api/transactions/history', 'GET'), true, 'RECORDS_PER_REQUEST', 500),
    (get_role_id('BUSINESS_ACCOUNT_HOLDER'), get_api_method_id('/api/transactions/*/details', 'GET'), true, NULL, NULL),
    (get_role_id('BUSINESS_ACCOUNT_HOLDER'), get_api_method_id('/api/transactions/report', 'GET'), true, 'MONTHLY_COUNT', 10),
    (get_role_id('BUSINESS_ACCOUNT_HOLDER'), get_api_method_id('/api/transactions/search', 'POST'), true, 'DAILY_COUNT', 50),
    (get_role_id('BUSINESS_ACCOUNT_HOLDER'), get_api_method_id('/api/transactions/*/dispute', 'POST'), true, 'MONTHLY_COUNT', 10),
    (get_role_id('BUSINESS_ACCOUNT_HOLDER'), get_api_method_id('/api/auth/logout', 'POST'), true, NULL, NULL),
    (get_role_id('BUSINESS_ACCOUNT_HOLDER'), get_api_method_id('/api/auth/refresh', 'POST'), true, NULL, NULL),
    (get_role_id('BUSINESS_ACCOUNT_HOLDER'), get_api_method_id('/api/auth/permissions', 'GET'), true, NULL, NULL),
    (get_role_id('BUSINESS_ACCOUNT_HOLDER'), get_api_method_id('/api/notifications/history', 'GET'), true, 'RECORDS_PER_REQUEST', 200),
    (get_role_id('BUSINESS_ACCOUNT_HOLDER'), get_api_method_id('/api/notifications/preferences', 'GET'), true, NULL, NULL),
    (get_role_id('BUSINESS_ACCOUNT_HOLDER'), get_api_method_id('/api/notifications/preferences', 'PUT'), true, NULL, NULL),
    (get_role_id('BUSINESS_ACCOUNT_HOLDER'), get_api_method_id('/api/reports/account-summary', 'GET'), true, NULL, NULL),
    (get_role_id('BUSINESS_ACCOUNT_HOLDER'), get_api_method_id('/api/reports/transaction-summary', 'GET'), true, NULL, NULL)
ON CONFLICT (role_id, method_id) DO NOTHING;

-- PREMIUM_ACCOUNT_HOLDER permissions (premium features)
INSERT INTO role_permissions (role_id, method_id, allowed, limit_type, limit_value) VALUES
    (get_role_id('PREMIUM_ACCOUNT_HOLDER'), get_api_method_id('/api/users/profile', 'GET'), true, NULL, NULL),
    (get_role_id('PREMIUM_ACCOUNT_HOLDER'), get_api_method_id('/api/users/profile', 'PUT'), true, NULL, NULL),
    (get_role_id('PREMIUM_ACCOUNT_HOLDER'), get_api_method_id('/api/payments/transfer', 'POST'), true, 'DAILY_AMOUNT', 1000000),
    (get_role_id('PREMIUM_ACCOUNT_HOLDER'), get_api_method_id('/api/payments/deposit', 'POST'), true, 'DAILY_AMOUNT', 2000000),
    (get_role_id('PREMIUM_ACCOUNT_HOLDER'), get_api_method_id('/api/payments/withdraw', 'POST'), true, 'DAILY_AMOUNT', 500000),
    (get_role_id('PREMIUM_ACCOUNT_HOLDER'), get_api_method_id('/api/payments/bulk-transfer', 'POST'), true, 'DAILY_COUNT', 200),
    (get_role_id('PREMIUM_ACCOUNT_HOLDER'), get_api_method_id('/api/payments/history', 'GET'), true, 'RECORDS_PER_REQUEST', 1000),
    (get_role_id('PREMIUM_ACCOUNT_HOLDER'), get_api_method_id('/api/payments/*/status', 'GET'), true, NULL, NULL),
    (get_role_id('PREMIUM_ACCOUNT_HOLDER'), get_api_method_id('/api/payments/*/cancel', 'POST'), true, NULL, NULL),
    (get_role_id('PREMIUM_ACCOUNT_HOLDER'), get_api_method_id('/api/transactions/history', 'GET'), true, 'RECORDS_PER_REQUEST', 1000),
    (get_role_id('PREMIUM_ACCOUNT_HOLDER'), get_api_method_id('/api/transactions/*/details', 'GET'), true, NULL, NULL),
    (get_role_id('PREMIUM_ACCOUNT_HOLDER'), get_api_method_id('/api/transactions/report', 'GET'), true, 'MONTHLY_COUNT', 20),
    (get_role_id('PREMIUM_ACCOUNT_HOLDER'), get_api_method_id('/api/transactions/search', 'POST'), true, 'DAILY_COUNT', 100),
    (get_role_id('PREMIUM_ACCOUNT_HOLDER'), get_api_method_id('/api/transactions/*/dispute', 'POST'), true, 'MONTHLY_COUNT', 15),
    (get_role_id('PREMIUM_ACCOUNT_HOLDER'), get_api_method_id('/api/auth/logout', 'POST'), true, NULL, NULL),
    (get_role_id('PREMIUM_ACCOUNT_HOLDER'), get_api_method_id('/api/auth/refresh', 'POST'), true, NULL, NULL),
    (get_role_id('PREMIUM_ACCOUNT_HOLDER'), get_api_method_id('/api/auth/permissions', 'GET'), true, NULL, NULL),
    (get_role_id('PREMIUM_ACCOUNT_HOLDER'), get_api_method_id('/api/notifications/history', 'GET'), true, 'RECORDS_PER_REQUEST', 500),
    (get_role_id('PREMIUM_ACCOUNT_HOLDER'), get_api_method_id('/api/notifications/preferences', 'GET'), true, NULL, NULL),
    (get_role_id('PREMIUM_ACCOUNT_HOLDER'), get_api_method_id('/api/notifications/preferences', 'PUT'), true, NULL, NULL),
    (get_role_id('PREMIUM_ACCOUNT_HOLDER'), get_api_method_id('/api/reports/account-summary', 'GET'), true, NULL, NULL),
    (get_role_id('PREMIUM_ACCOUNT_HOLDER'), get_api_method_id('/api/reports/transaction-summary', 'GET'), true, NULL, NULL)
ON CONFLICT (role_id, method_id) DO NOTHING;

-- EMPLOYEE permissions (internal staff operations)
INSERT INTO role_permissions (role_id, method_id, allowed, limit_type, limit_value) VALUES
    (get_role_id('EMPLOYEE'), get_api_method_id('/api/users/profile', 'GET'), true, NULL, NULL),
    (get_role_id('EMPLOYEE'), get_api_method_id('/api/users/profile', 'PUT'), true, NULL, NULL),
    (get_role_id('EMPLOYEE'), get_api_method_id('/api/users/*/status', 'PUT'), true, NULL, NULL),
    (get_role_id('EMPLOYEE'), get_api_method_id('/api/users/*/roles', 'GET'), true, NULL, NULL),
    (get_role_id('EMPLOYEE'), get_api_method_id('/api/payments/*/status', 'GET'), true, NULL, NULL),
    (get_role_id('EMPLOYEE'), get_api_method_id('/api/payments/*/cancel', 'POST'), true, NULL, NULL),
    (get_role_id('EMPLOYEE'), get_api_method_id('/api/transactions/history', 'GET'), true, 'RECORDS_PER_REQUEST', 500),
    (get_role_id('EMPLOYEE'), get_api_method_id('/api/transactions/*/details', 'GET'), true, NULL, NULL),
    (get_role_id('EMPLOYEE'), get_api_method_id('/api/transactions/search', 'POST'), true, 'DAILY_COUNT', 100),
    (get_role_id('EMPLOYEE'), get_api_method_id('/api/auth/logout', 'POST'), true, NULL, NULL),
    (get_role_id('EMPLOYEE'), get_api_method_id('/api/auth/refresh', 'POST'), true, NULL, NULL),
    (get_role_id('EMPLOYEE'), get_api_method_id('/api/auth/permissions', 'GET'), true, NULL, NULL),
    (get_role_id('EMPLOYEE'), get_api_method_id('/api/notifications/send', 'POST'), true, 'DAILY_COUNT', 100),
    (get_role_id('EMPLOYEE'), get_api_method_id('/api/notifications/history', 'GET'), true, 'RECORDS_PER_REQUEST', 200),
    (get_role_id('EMPLOYEE'), get_api_method_id('/api/reports/account-summary', 'GET'), true, NULL, NULL),
    (get_role_id('EMPLOYEE'), get_api_method_id('/api/reports/transaction-summary', 'GET'), true, NULL, NULL)
ON CONFLICT (role_id, method_id) DO NOTHING;

-- Drop helper functions
DROP FUNCTION get_role_id(VARCHAR);
DROP FUNCTION get_api_method_id(VARCHAR, VARCHAR);
