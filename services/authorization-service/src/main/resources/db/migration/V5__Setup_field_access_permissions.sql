-- Set up field access permissions for different roles
-- This migration defines what fields each role can access for different resource types

-- Helper function to get role ID by name
CREATE OR REPLACE FUNCTION get_role_id(role_name VARCHAR) RETURNS BIGINT AS $$
DECLARE
    result BIGINT;
BEGIN
    SELECT role_id INTO result FROM roles WHERE name = role_name;
    RETURN result;
END;
$$ LANGUAGE plpgsql;

-- ADMIN field access (full access to all fields)
INSERT INTO field_access (role_id, resource_type, allowed_fields, config) VALUES
    (get_role_id('ADMIN'), 'user', '["*"]', '{"access_level": "full", "can_modify": true}'),
    (get_role_id('ADMIN'), 'account', '["*"]', '{"access_level": "full", "can_modify": true}'),
    (get_role_id('ADMIN'), 'transaction', '["*"]', '{"access_level": "full", "can_modify": true}'),
    (get_role_id('ADMIN'), 'payment', '["*"]', '{"access_level": "full", "can_modify": true}'),
    (get_role_id('ADMIN'), 'notification', '["*"]', '{"access_level": "full", "can_modify": true}'),
    (get_role_id('ADMIN'), 'report', '["*"]', '{"access_level": "full", "can_modify": true}')
ON CONFLICT (role_id, resource_type) DO NOTHING;

-- ACCOUNT_HOLDER field access (own data only)
INSERT INTO field_access (role_id, resource_type, allowed_fields, config) VALUES
    (get_role_id('ACCOUNT_HOLDER'), 'user', 
     '["id", "username", "email", "firstName", "lastName", "phoneNumber", "dateOfBirth", "address", "profilePicture", "lastLoginAt", "isActive"]', 
     '{"access_level": "self_only", "can_modify": ["email", "phoneNumber", "address", "profilePicture"], "read_only": ["id", "username", "lastLoginAt", "isActive"]}'),
    
    (get_role_id('ACCOUNT_HOLDER'), 'account', 
     '["accountNumber", "accountType", "balance", "currency", "status", "createdAt", "lastTransactionAt"]', 
     '{"access_level": "self_only", "can_modify": [], "read_only": ["accountNumber", "accountType", "balance", "currency", "status", "createdAt", "lastTransactionAt"]}'),
    
    (get_role_id('ACCOUNT_HOLDER'), 'transaction', 
     '["transactionId", "fromAccount", "toAccount", "amount", "currency", "type", "status", "description", "createdAt", "completedAt"]', 
     '{"access_level": "self_only", "can_modify": [], "read_only": ["transactionId", "fromAccount", "toAccount", "amount", "currency", "type", "status", "description", "createdAt", "completedAt"]}'),
    
    (get_role_id('ACCOUNT_HOLDER'), 'payment', 
     '["paymentId", "fromAccount", "toAccount", "amount", "currency", "status", "description", "scheduledAt", "processedAt"]', 
     '{"access_level": "self_only", "can_modify": ["description"], "read_only": ["paymentId", "fromAccount", "toAccount", "amount", "currency", "status", "scheduledAt", "processedAt"]}'),
    
    (get_role_id('ACCOUNT_HOLDER'), 'notification', 
     '["notificationId", "type", "title", "message", "status", "createdAt", "readAt"]', 
     '{"access_level": "self_only", "can_modify": ["status", "readAt"], "read_only": ["notificationId", "type", "title", "message", "createdAt"]}')
ON CONFLICT (role_id, resource_type) DO NOTHING;

-- BUSINESS_ACCOUNT_HOLDER field access (enhanced business features)
INSERT INTO field_access (role_id, resource_type, allowed_fields, config) VALUES
    (get_role_id('BUSINESS_ACCOUNT_HOLDER'), 'user', 
     '["id", "username", "email", "firstName", "lastName", "phoneNumber", "dateOfBirth", "address", "profilePicture", "lastLoginAt", "isActive", "businessName", "taxId", "businessType"]', 
     '{"access_level": "self_only", "can_modify": ["email", "phoneNumber", "address", "profilePicture", "businessName", "businessType"], "read_only": ["id", "username", "lastLoginAt", "isActive", "taxId"]}'),
    
    (get_role_id('BUSINESS_ACCOUNT_HOLDER'), 'account', 
     '["accountNumber", "accountType", "balance", "currency", "status", "createdAt", "lastTransactionAt", "dailyLimit", "monthlyLimit", "businessCategory"]', 
     '{"access_level": "self_only", "can_modify": [], "read_only": ["accountNumber", "accountType", "balance", "currency", "status", "createdAt", "lastTransactionAt", "dailyLimit", "monthlyLimit", "businessCategory"]}'),
    
    (get_role_id('BUSINESS_ACCOUNT_HOLDER'), 'transaction', 
     '["transactionId", "fromAccount", "toAccount", "amount", "currency", "type", "status", "description", "createdAt", "completedAt", "reference", "businessPurpose"]', 
     '{"access_level": "self_only", "can_modify": [], "read_only": ["transactionId", "fromAccount", "toAccount", "amount", "currency", "type", "status", "description", "createdAt", "completedAt", "reference", "businessPurpose"]}'),
    
    (get_role_id('BUSINESS_ACCOUNT_HOLDER'), 'payment', 
     '["paymentId", "fromAccount", "toAccount", "amount", "currency", "status", "description", "scheduledAt", "processedAt", "batchId", "reference"]', 
     '{"access_level": "self_only", "can_modify": ["description", "reference"], "read_only": ["paymentId", "fromAccount", "toAccount", "amount", "currency", "status", "scheduledAt", "processedAt", "batchId"]}'),
    
    (get_role_id('BUSINESS_ACCOUNT_HOLDER'), 'report', 
     '["reportId", "type", "dateRange", "accountNumbers", "totalTransactions", "totalAmount", "generatedAt"]', 
     '{"access_level": "self_only", "can_modify": [], "read_only": ["reportId", "type", "dateRange", "accountNumbers", "totalTransactions", "totalAmount", "generatedAt"]}')
ON CONFLICT (role_id, resource_type) DO NOTHING;

-- EMPLOYEE field access (customer service operations)
INSERT INTO field_access (role_id, resource_type, allowed_fields, config) VALUES
    (get_role_id('EMPLOYEE'), 'user', 
     '["id", "username", "email", "firstName", "lastName", "phoneNumber", "address", "lastLoginAt", "isActive", "status"]', 
     '{"access_level": "customer_service", "can_modify": ["status"], "read_only": ["id", "username", "email", "firstName", "lastName", "phoneNumber", "address", "lastLoginAt", "isActive"], "mask_sensitive": ["phoneNumber"]}'),
    
    (get_role_id('EMPLOYEE'), 'account', 
     '["accountNumber", "accountType", "status", "createdAt", "lastTransactionAt"]', 
     '{"access_level": "customer_service", "can_modify": ["status"], "read_only": ["accountNumber", "accountType", "createdAt", "lastTransactionAt"], "mask_sensitive": ["accountNumber"]}'),
    
    (get_role_id('EMPLOYEE'), 'transaction', 
     '["transactionId", "fromAccount", "toAccount", "amount", "currency", "type", "status", "description", "createdAt"]', 
     '{"access_level": "customer_service", "can_modify": [], "read_only": ["transactionId", "fromAccount", "toAccount", "amount", "currency", "type", "status", "description", "createdAt"], "mask_sensitive": ["fromAccount", "toAccount"]}'),
    
    (get_role_id('EMPLOYEE'), 'payment', 
     '["paymentId", "status", "description", "scheduledAt", "processedAt"]', 
     '{"access_level": "customer_service", "can_modify": ["status"], "read_only": ["paymentId", "description", "scheduledAt", "processedAt"]}')
ON CONFLICT (role_id, resource_type) DO NOTHING;

-- MANAGER field access (management operations)
INSERT INTO field_access (role_id, resource_type, allowed_fields, config) VALUES
    (get_role_id('MANAGER'), 'user', 
     '["id", "username", "email", "firstName", "lastName", "phoneNumber", "address", "lastLoginAt", "isActive", "status", "roles"]', 
     '{"access_level": "management", "can_modify": ["status", "roles"], "read_only": ["id", "username", "email", "firstName", "lastName", "phoneNumber", "address", "lastLoginAt", "isActive"]}'),
    
    (get_role_id('MANAGER'), 'account', 
     '["accountNumber", "accountType", "balance", "currency", "status", "createdAt", "lastTransactionAt", "dailyLimit", "monthlyLimit"]', 
     '{"access_level": "management", "can_modify": ["status", "dailyLimit", "monthlyLimit"], "read_only": ["accountNumber", "accountType", "balance", "currency", "createdAt", "lastTransactionAt"]}'),
    
    (get_role_id('MANAGER'), 'transaction', 
     '["transactionId", "fromAccount", "toAccount", "amount", "currency", "type", "status", "description", "createdAt", "completedAt"]', 
     '{"access_level": "management", "can_modify": ["status"], "read_only": ["transactionId", "fromAccount", "toAccount", "amount", "currency", "type", "description", "createdAt", "completedAt"]}'),
    
    (get_role_id('MANAGER'), 'report', 
     '["reportId", "type", "dateRange", "accountNumbers", "totalTransactions", "totalAmount", "generatedAt", "summary"]', 
     '{"access_level": "management", "can_modify": [], "read_only": ["reportId", "type", "dateRange", "accountNumbers", "totalTransactions", "totalAmount", "generatedAt", "summary"]}')
ON CONFLICT (role_id, resource_type) DO NOTHING;

-- AUDITOR field access (audit and compliance)
INSERT INTO field_access (role_id, resource_type, allowed_fields, config) VALUES
    (get_role_id('AUDITOR'), 'user', 
     '["id", "username", "lastLoginAt", "isActive", "status", "roles", "createdAt"]', 
     '{"access_level": "audit", "can_modify": [], "read_only": ["id", "username", "lastLoginAt", "isActive", "status", "roles", "createdAt"]}'),
    
    (get_role_id('AUDITOR'), 'account', 
     '["accountNumber", "accountType", "status", "createdAt", "lastTransactionAt"]', 
     '{"access_level": "audit", "can_modify": [], "read_only": ["accountNumber", "accountType", "status", "createdAt", "lastTransactionAt"], "mask_sensitive": ["accountNumber"]}'),
    
    (get_role_id('AUDITOR'), 'transaction', 
     '["transactionId", "amount", "currency", "type", "status", "createdAt", "completedAt", "auditTrail"]', 
     '{"access_level": "audit", "can_modify": [], "read_only": ["transactionId", "amount", "currency", "type", "status", "createdAt", "completedAt", "auditTrail"]}'),
    
    (get_role_id('AUDITOR'), 'report', 
     '["reportId", "type", "dateRange", "totalTransactions", "totalAmount", "generatedAt", "auditData"]', 
     '{"access_level": "audit", "can_modify": [], "read_only": ["reportId", "type", "dateRange", "totalTransactions", "totalAmount", "generatedAt", "auditData"]}')
ON CONFLICT (role_id, resource_type) DO NOTHING;

-- Drop helper function
DROP FUNCTION get_role_id(VARCHAR);
