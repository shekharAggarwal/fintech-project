-- Set up role permissions mapping
-- PostgreSQL-safe with TEXT types, wildcard support, and idempotency

-- ADMIN role permissions (full access)
INSERT INTO role_permissions (role_id, method_id, allowed, limit_type, limit_value)
SELECT r.role_id, am.api_method_id, true, NULL, NULL
FROM roles r, api_methods am
WHERE r.name = 'ADMIN'
ON CONFLICT (role_id, method_id) DO NOTHING;

-- ACCOUNT_HOLDER permissions (basic user operations)
INSERT INTO role_permissions (role_id, method_id, allowed, limit_type, limit_value)
SELECT r.role_id, am.api_method_id, true, 'DAILY_AMOUNT', 50000
FROM roles r, api_methods am
WHERE r.name = 'ACCOUNT_HOLDER' AND am.path = '/api/payments/transfer' AND am.http_method = 'POST'
ON CONFLICT (role_id, method_id) DO NOTHING;

INSERT INTO role_permissions (role_id, method_id, allowed, limit_type, limit_value)
SELECT r.role_id, am.api_method_id, true, 'DAILY_AMOUNT', 100000
FROM roles r, api_methods am
WHERE r.name = 'ACCOUNT_HOLDER' AND am.path = '/api/payments/deposit' AND am.http_method = 'POST'
ON CONFLICT (role_id, method_id) DO NOTHING;

INSERT INTO role_permissions (role_id, method_id, allowed, limit_type, limit_value)
SELECT r.role_id, am.api_method_id, true, 'DAILY_AMOUNT', 25000
FROM roles r, api_methods am
WHERE r.name = 'ACCOUNT_HOLDER' AND am.path = '/api/payments/withdraw' AND am.http_method = 'POST'
ON CONFLICT (role_id, method_id) DO NOTHING;

INSERT INTO role_permissions (role_id, method_id, allowed, limit_type, limit_value)
SELECT r.role_id, am.api_method_id, true, NULL, NULL
FROM roles r, api_methods am
WHERE r.name = 'ACCOUNT_HOLDER' 
AND am.path IN ('/api/users/profile', '/api/payments/history', '/api/payments/*/status', '/api/payments/*/cancel', 
                '/api/transactions/history', '/api/transactions/*/details', '/api/auth/logout', '/api/auth/refresh', 
                '/api/auth/permissions', '/api/notifications/history', '/api/notifications/preferences',
                '/api/reports/account-summary')
ON CONFLICT (role_id, method_id) DO NOTHING;

-- BUSINESS_ACCOUNT_HOLDER permissions (enhanced business operations)
INSERT INTO role_permissions (role_id, method_id, allowed, limit_type, limit_value)
SELECT r.role_id, am.api_method_id, true, 'DAILY_AMOUNT', 500000
FROM roles r, api_methods am
WHERE r.name = 'BUSINESS_ACCOUNT_HOLDER' AND am.path = '/api/payments/transfer' AND am.http_method = 'POST'
ON CONFLICT (role_id, method_id) DO NOTHING;

INSERT INTO role_permissions (role_id, method_id, allowed, limit_type, limit_value)
SELECT r.role_id, am.api_method_id, true, 'DAILY_AMOUNT', 1000000
FROM roles r, api_methods am
WHERE r.name = 'BUSINESS_ACCOUNT_HOLDER' AND am.path = '/api/payments/deposit' AND am.http_method = 'POST'
ON CONFLICT (role_id, method_id) DO NOTHING;

INSERT INTO role_permissions (role_id, method_id, allowed, limit_type, limit_value)
SELECT r.role_id, am.api_method_id, true, NULL, NULL
FROM roles r, api_methods am
WHERE r.name = 'BUSINESS_ACCOUNT_HOLDER' 
AND am.path IN ('/api/users/profile', '/api/payments/bulk-transfer', '/api/payments/history', '/api/payments/*/status', 
                '/api/payments/*/cancel', '/api/transactions/history', '/api/transactions/*/details', 
                '/api/transactions/report', '/api/transactions/search', '/api/auth/logout', '/api/auth/refresh', 
                '/api/auth/permissions', '/api/notifications/history', '/api/notifications/preferences',
                '/api/reports/account-summary', '/api/reports/transaction-summary')
ON CONFLICT (role_id, method_id) DO NOTHING;

-- EMPLOYEE permissions (internal staff operations)
INSERT INTO role_permissions (role_id, method_id, allowed, limit_type, limit_value)
SELECT r.role_id, am.api_method_id, true, NULL, NULL
FROM roles r, api_methods am
WHERE r.name = 'EMPLOYEE' 
AND am.path IN ('/api/users/profile', '/api/users/*/status', '/api/users/*/roles', '/api/payments/*/status',
                '/api/payments/*/cancel', '/api/transactions/history', '/api/transactions/*/details',
                '/api/transactions/search', '/api/auth/logout', '/api/auth/refresh', '/api/auth/permissions',
                '/api/notifications/send', '/api/notifications/history', '/api/reports/account-summary',
                '/api/reports/transaction-summary')
ON CONFLICT (role_id, method_id) DO NOTHING;
