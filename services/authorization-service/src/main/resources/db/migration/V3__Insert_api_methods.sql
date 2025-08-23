-- Insert API methods for fintech operations
-- This migration adds common API endpoints that need authorization

-- User Service APIs
INSERT INTO api_methods (path, http_method, description) VALUES
    ('/api/users/profile', 'GET', 'Get user profile information'),
    ('/api/users/profile', 'PUT', 'Update user profile information'),
    ('/api/users/register', 'POST', 'Register new user'),
    ('/api/users/all', 'GET', 'Get all users (admin only)'),
    ('/api/users/*/status', 'PUT', 'Update user status'),
    ('/api/users/*/roles', 'GET', 'Get user roles'),
    ('/api/users/*/roles', 'POST', 'Assign role to user'),
    ('/api/users/*/roles/*', 'DELETE', 'Remove role from user'),

-- Payment Service APIs
    ('/api/payments/transfer', 'POST', 'Transfer money between accounts'),
    ('/api/payments/deposit', 'POST', 'Deposit money to account'),
    ('/api/payments/withdraw', 'POST', 'Withdraw money from account'),
    ('/api/payments/history', 'GET', 'Get payment history'),
    ('/api/payments/*/status', 'GET', 'Get payment status'),
    ('/api/payments/*/cancel', 'POST', 'Cancel pending payment'),
    ('/api/payments/bulk-transfer', 'POST', 'Bulk transfer operations'),

-- Transaction Service APIs
    ('/api/transactions/history', 'GET', 'Get transaction history'),
    ('/api/transactions/*/details', 'GET', 'Get transaction details'),
    ('/api/transactions/report', 'GET', 'Generate transaction report'),
    ('/api/transactions/search', 'POST', 'Search transactions'),
    ('/api/transactions/*/dispute', 'POST', 'Raise transaction dispute'),

-- Authorization Service APIs
    ('/api/auth/login', 'POST', 'User login'),
    ('/api/auth/logout', 'POST', 'User logout'),
    ('/api/auth/refresh', 'POST', 'Refresh JWT token'),
    ('/api/auth/permissions', 'GET', 'Get user permissions'),
    ('/api/auth/roles', 'GET', 'Get all roles'),
    ('/api/auth/roles', 'POST', 'Create new role'),
    ('/api/auth/roles/*/permissions', 'GET', 'Get role permissions'),
    ('/api/auth/roles/*/permissions', 'POST', 'Update role permissions'),

-- Notification Service APIs
    ('/api/notifications/send', 'POST', 'Send notification'),
    ('/api/notifications/history', 'GET', 'Get notification history'),
    ('/api/notifications/preferences', 'GET', 'Get notification preferences'),
    ('/api/notifications/preferences', 'PUT', 'Update notification preferences'),

-- Reporting Service APIs
    ('/api/reports/account-summary', 'GET', 'Get account summary report'),
    ('/api/reports/transaction-summary', 'GET', 'Get transaction summary report'),
    ('/api/reports/compliance', 'GET', 'Get compliance report'),
    ('/api/reports/audit-trail', 'GET', 'Get audit trail report'),
    ('/api/reports/risk-assessment', 'GET', 'Get risk assessment report'),

-- Scheduler Service APIs
    ('/api/scheduler/jobs', 'GET', 'Get scheduled jobs'),
    ('/api/scheduler/jobs', 'POST', 'Create scheduled job'),
    ('/api/scheduler/jobs/*', 'PUT', 'Update scheduled job'),
    ('/api/scheduler/jobs/*', 'DELETE', 'Delete scheduled job'),

-- Retry Service APIs
    ('/api/retry/failed-operations', 'GET', 'Get failed operations'),
    ('/api/retry/*/retry', 'POST', 'Retry failed operation'),
    ('/api/retry/*/cancel', 'POST', 'Cancel retry operation')

ON CONFLICT (path) DO NOTHING;
