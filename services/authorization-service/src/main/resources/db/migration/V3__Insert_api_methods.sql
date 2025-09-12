-- Insert API methods for fintech operations
-- Updated to use composite conflict target (path, http_method)

INSERT INTO api_methods (path, http_method, description) VALUES
-- User Service APIs
('/api/user/profile/me', 'GET', 'Get current user own profile'),
('/api/user/profile/me', 'PUT', 'Update current user own profile'),
('/api/user/secured/profile/*', 'GET', 'Get filtered profile data as map'),
('/api/user/secured/profile/*', 'PUT', 'Update filtered profile data as map'),
('/api/user/search', 'GET', 'Search users by name, phone, email, or account number'),
('/api/user/role/*', 'PUT', 'Update user role (admin only)'),
('/api/user/health', 'GET', 'Health check endpoint'),

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

ON CONFLICT (path, http_method) DO NOTHING;
