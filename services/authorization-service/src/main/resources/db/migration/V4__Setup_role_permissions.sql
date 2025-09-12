-- ADMIN role permissions (full access)
INSERT INTO role_permissions (role_id, method_id, allowed)
SELECT r.role_id, am.api_method_id, true
FROM roles r,
     api_methods am
WHERE r.name = 'ADMIN' ON CONFLICT (role_id, method_id) DO NOTHING;


-- ACCOUNT_HOLDER permissions (basic user operations)
INSERT INTO role_permissions (role_id, method_id, allowed)
SELECT r.role_id, am.api_method_id, true
FROM roles r,
     api_methods am
WHERE r.name = 'ACCOUNT_HOLDER'
  AND am.path = '/api/payments/transfer'
  AND am.http_method = 'POST' ON CONFLICT (role_id, method_id) DO NOTHING;

INSERT INTO role_permissions (role_id, method_id, allowed)
SELECT r.role_id, am.api_method_id, true
FROM roles r,
     api_methods am
WHERE r.name = 'ACCOUNT_HOLDER'
  AND am.path = '/api/payments/deposit'
  AND am.http_method = 'POST' ON CONFLICT (role_id, method_id) DO NOTHING;

INSERT INTO role_permissions (role_id, method_id, allowed)
SELECT r.role_id, am.api_method_id, true
FROM roles r,
     api_methods am
WHERE r.name = 'ACCOUNT_HOLDER'
  AND am.path = '/api/payments/withdraw'
  AND am.http_method = 'POST' ON CONFLICT (role_id, method_id) DO NOTHING;

INSERT INTO role_permissions (role_id, method_id, allowed)
SELECT r.role_id, am.api_method_id, true
FROM roles r,
     api_methods am
WHERE r.name = 'ACCOUNT_HOLDER'
  AND am.path IN ('/api/user/profile/me',
                  '/api/payments/history',
                  '/api/payments/*/status',
                  '/api/payments/*/cancel',
                  '/api/transactions/history',
                  '/api/transactions/*/details',
                  '/api/auth/logout',
                  '/api/auth/refresh',
                  '/api/auth/permissions',
                  '/api/notifications/history',
                  '/api/notifications/preferences',
                  '/api/reports/account-summary')
    ON CONFLICT (role_id, method_id) DO NOTHING;

-- BUSINESS_ACCOUNT_HOLDER permissions (enhanced business operations)
INSERT INTO role_permissions (role_id, method_id, allowed)
SELECT r.role_id, am.api_method_id, true
FROM roles r,
     api_methods am
WHERE r.name = 'BUSINESS_ACCOUNT_HOLDER'
  AND am.path = '/api/payments/transfer'
  AND am.http_method = 'POST' ON CONFLICT (role_id, method_id) DO NOTHING;

INSERT INTO role_permissions (role_id, method_id, allowed)
SELECT r.role_id, am.api_method_id, true
FROM roles r,
     api_methods am
WHERE r.name = 'BUSINESS_ACCOUNT_HOLDER'
  AND am.path = '/api/payments/deposit'
  AND am.http_method = 'POST' ON CONFLICT (role_id, method_id) DO NOTHING;

INSERT INTO role_permissions (role_id, method_id, allowed)
SELECT r.role_id, am.api_method_id, true
FROM roles r,
     api_methods am
WHERE r.name = 'BUSINESS_ACCOUNT_HOLDER'
  AND am.path IN ('/api/user/profile', '/api/user/profile/me', '/api/user/profile/*/filtered', '/api/user/health',
                  '/api/payments/bulk-transfer', '/api/payments/history', '/api/payments/*/status',
                  '/api/payments/*/cancel', '/api/transactions/history', '/api/transactions/*/details',
                  '/api/transactions/report', '/api/transactions/search', '/api/auth/logout', '/api/auth/refresh',
                  '/api/auth/permissions', '/api/notifications/history', '/api/notifications/preferences',
                  '/api/reports/account-summary', '/api/reports/transaction-summary')
    ON CONFLICT (role_id, method_id) DO NOTHING;

-- EMPLOYEE permissions (internal staff operations)
INSERT INTO role_permissions (role_id, method_id, allowed)
SELECT r.role_id, am.api_method_id, true
FROM roles r,
     api_methods am
WHERE r.name = 'EMPLOYEE'
  AND am.path IN ('/api/user/profile', '/api/user/profile/me', '/api/user/profile/*/filtered', '/api/user/search',
                  '/api/user/health',
                  '/api/user/*/status', '/api/user/*/roles', '/api/payments/*/status',
                  '/api/payments/*/cancel', '/api/transactions/history', '/api/transactions/*/details',
                  '/api/transactions/search', '/api/auth/logout', '/api/auth/refresh', '/api/auth/permissions',
                  '/api/notifications/send', '/api/notifications/history', '/api/reports/account-summary',
                  '/api/reports/transaction-summary')
    ON CONFLICT (role_id, method_id) DO NOTHING;

-- MANAGER permissions (enhanced user management capabilities)
INSERT INTO role_permissions (role_id, method_id, allowed)
SELECT r.role_id, am.api_method_id, true
FROM roles r,
     api_methods am
WHERE r.name = 'MANAGER'
  AND am.path IN ('/api/user/profile', '/api/user/profile/me', '/api/user/profile/*/filtered', '/api/user/search',
                  '/api/user/health',
                  '/api/user/*/status', '/api/user/*/roles', '/api/user/role/*')
    ON CONFLICT (role_id, method_id) DO NOTHING;

-- CUSTOMER_SUPPORT permissions (can search and view user profiles for support)
INSERT INTO role_permissions (role_id, method_id, allowed)
SELECT r.role_id, am.api_method_id, true
FROM roles r,
     api_methods am
WHERE r.name = 'CUSTOMER_SUPPORT'
  AND am.path IN ('/api/user/profile', '/api/user/profile/me', '/api/user/search',
                  '/api/user/health')
    ON CONFLICT (role_id, method_id) DO NOTHING;

-- AUDITOR permissions (read-only access to user data)
INSERT INTO role_permissions (role_id, method_id, allowed)
SELECT r.role_id, am.api_method_id, true
FROM roles r,
     api_methods am
WHERE r.name = 'AUDITOR'
  AND am.path IN ('/api/user/profile', '/api/user/profile/me', '/api/user/profile/*/filtered', '/api/user/search',
                  '/api/user/health')
    ON CONFLICT (role_id, method_id) DO NOTHING;

-- COMPLIANCE_OFFICER permissions (comprehensive user data access)
INSERT INTO role_permissions (role_id, method_id, allowed)
SELECT r.role_id, am.api_method_id, true
FROM roles r,
     api_methods am
WHERE r.name = 'COMPLIANCE_OFFICER'
  AND am.path IN ('/api/user/profile', '/api/user/profile/me', '/api/user/profile/*/filtered', '/api/user/search',
                  '/api/user/health')
    ON CONFLICT (role_id, method_id) DO NOTHING;

-- PREMIUM_ACCOUNT_HOLDER permissions (enhanced profile access)
INSERT INTO role_permissions (role_id, method_id, allowed)
SELECT r.role_id, am.api_method_id, true
FROM roles r,
     api_methods am
WHERE r.name = 'PREMIUM_ACCOUNT_HOLDER'
  AND am.path IN ('/api/user/profile', '/api/user/profile/me', '/api/user/profile/*/filtered',
                  '/api/user/health')
    ON CONFLICT (role_id, method_id) DO NOTHING;
