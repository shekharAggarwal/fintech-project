-- Insert predefined roles for the fintech system
-- Idempotent inserts

INSERT INTO roles (name, description) VALUES
                                          ('ADMIN', 'System administrator with full access to all resources and operations'),
                                          ('MANAGER', 'Branch/Department manager with elevated privileges for user and account management'),
                                          ('EMPLOYEE', 'Bank employee with access to customer service and basic operations'),
                                          ('ACCOUNT_HOLDER', 'Regular customer with access to their own account operations'),
                                          ('BUSINESS_ACCOUNT_HOLDER', 'Business customer with enhanced account features and transaction limits'),
                                          ('PREMIUM_ACCOUNT_HOLDER', 'Premium customer with additional services and higher transaction limits'),
                                          ('AUDITOR', 'Internal/External auditor with read-only access to transaction and account data'),
                                          ('COMPLIANCE_OFFICER', 'Compliance officer with access to monitoring and reporting features'),
                                          ('RISK_ANALYST', 'Risk management analyst with access to risk assessment and reporting tools'),
                                          ('CUSTOMER_SUPPORT', 'Customer support representative with limited account access for assistance'),
                                          ('TELLER', 'Bank teller with access to basic banking operations and cash handling'),
                                          ('LOAN_OFFICER', 'Loan officer with access to loan processing and approval workflows'),
                                          ('INVESTMENT_ADVISOR', 'Investment advisor with access to portfolio management and investment tools'),
                                          ('TREASURY_OFFICER', 'Treasury officer with access to liquidity management and treasury operations'),
                                          ('SECURITY_OFFICER', 'Security officer with access to security monitoring and incident management')
    ON CONFLICT (name) DO NOTHING;
