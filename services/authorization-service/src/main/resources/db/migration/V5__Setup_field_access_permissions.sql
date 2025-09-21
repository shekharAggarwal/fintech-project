-- ADMIN field access (full access to all fields)
INSERT INTO field_access (role_id, resource_type, allowed_fields, config)
SELECT r.role_id, 'user', '["*"]'::jsonb, '{"access_level": "full", "can_modify": true}'::jsonb
FROM roles r
WHERE r.name = 'ADMIN' ON CONFLICT (role_id, resource_type) DO NOTHING;

INSERT INTO field_access (role_id, resource_type, allowed_fields, config)
SELECT r.role_id, 'account', '["*"]'::jsonb, '{"access_level": "full", "can_modify": true}'::jsonb
FROM roles r
WHERE r.name = 'ADMIN' ON CONFLICT (role_id, resource_type) DO NOTHING;

-- ACCOUNT_HOLDER field access (own data only)
INSERT INTO field_access (role_id, resource_type, allowed_fields, config)
SELECT r.role_id,
       'user',
       '["id","username","email","firstName","lastName","phoneNumber"]'::jsonb, '{"access_level":"self_only","can_modify":["email","phoneNumber"]}'::jsonb
FROM roles r
WHERE r.name = 'ACCOUNT_HOLDER' ON CONFLICT (role_id, resource_type) DO NOTHING;

INSERT INTO field_access (role_id, resource_type, allowed_fields, config)
SELECT r.role_id,
       'account',
       '["accountNumber","accountType","balance","currency","status"]'::jsonb, '{"access_level":"self_only","can_modify":[]}'::jsonb
FROM roles r
WHERE r.name = 'ACCOUNT_HOLDER' ON CONFLICT (role_id, resource_type) DO NOTHING;

-- ACCOUNT_HOLDER payment field access (payment operations)
INSERT INTO field_access (role_id, resource_type, allowed_fields, config)
SELECT r.role_id,
       'payment',
       '["paymentId","fromAccount","toAccount","amount","currency","status","createdAt","updatedAt","description","reference"]'::jsonb, '{"access_level":"self_only","can_modify":[],"filter_by_user":true}'::jsonb
FROM roles r
WHERE r.name = 'ACCOUNT_HOLDER' ON CONFLICT (role_id, resource_type) DO NOTHING;

-- BUSINESS_ACCOUNT_HOLDER field access (enhanced business features)
INSERT INTO field_access (role_id, resource_type, allowed_fields, config)
SELECT r.role_id,
       'user',
       '["id","username","email","firstName","lastName","phoneNumber","status","accountType","tier","createdAt","lastLoginAt"]'::jsonb, '{"access_level":"enhanced_self","can_modify":["email","phoneNumber"],"dynamic_filtering":true}'::jsonb
FROM roles r
WHERE r.name = 'BUSINESS_ACCOUNT_HOLDER' ON CONFLICT (role_id, resource_type) DO NOTHING;

-- BUSINESS_ACCOUNT_HOLDER account field access
INSERT INTO field_access (role_id, resource_type, allowed_fields, config)
SELECT r.role_id,
       'account',
       '["accountNumber","accountType","balance","currency","status","businessName","taxId","registrationNumber"]'::jsonb, '{"access_level":"enhanced_self","can_modify":["businessName"]}'::jsonb
FROM roles r
WHERE r.name = 'BUSINESS_ACCOUNT_HOLDER' ON CONFLICT (role_id, resource_type) DO NOTHING;

-- BUSINESS_ACCOUNT_HOLDER payment field access (enhanced business payment features)
INSERT INTO field_access (role_id, resource_type, allowed_fields, config)
SELECT r.role_id,
       'payment',
       '["paymentId","fromAccount","toAccount","amount","currency","status","createdAt","updatedAt","description","reference","batchId","businessCategory","taxInformation"]'::jsonb, '{"access_level":"enhanced_self","can_modify":["description","reference"],"filter_by_user":true,"bulk_operations":true}'::jsonb
FROM roles r
WHERE r.name = 'BUSINESS_ACCOUNT_HOLDER' ON CONFLICT (role_id, resource_type) DO NOTHING;

-- PREMIUM_ACCOUNT_HOLDER field access (premium features)
INSERT INTO field_access (role_id, resource_type, allowed_fields, config)
SELECT r.role_id,
       'user',
       '["id","username","email","firstName","lastName","phoneNumber","status","accountType","tier","createdAt","lastLoginAt"]'::jsonb, '{"access_level":"premium_self","can_modify":["email","phoneNumber"],"dynamic_filtering":true}'::jsonb
FROM roles r
WHERE r.name = 'PREMIUM_ACCOUNT_HOLDER' ON CONFLICT (role_id, resource_type) DO NOTHING;

-- PREMIUM_ACCOUNT_HOLDER account field access
INSERT INTO field_access (role_id, resource_type, allowed_fields, config)
SELECT r.role_id,
       'account',
       '["accountNumber","accountType","balance","currency","status","premiumTier","rewardPoints","creditLimit"]'::jsonb, '{"access_level":"premium_self","can_modify":[]}'::jsonb
FROM roles r
WHERE r.name = 'PREMIUM_ACCOUNT_HOLDER' ON CONFLICT (role_id, resource_type) DO NOTHING;

-- PREMIUM_ACCOUNT_HOLDER payment field access (premium payment features)
INSERT INTO field_access (role_id, resource_type, allowed_fields, config)
SELECT r.role_id,
       'payment',
       '["paymentId","fromAccount","toAccount","amount","currency","status","createdAt","updatedAt","description","reference","rewardPoints","premiumFees","priorityProcessing"]'::jsonb, '{"access_level":"premium_self","can_modify":["description","reference"],"filter_by_user":true,"premium_features":true}'::jsonb
FROM roles r
WHERE r.name = 'PREMIUM_ACCOUNT_HOLDER' ON CONFLICT (role_id, resource_type) DO NOTHING;

-- EMPLOYEE field access (customer service operations)
INSERT INTO field_access (role_id, resource_type, allowed_fields, config)
SELECT r.role_id,
       'user',
       '["id","username","email","firstName","lastName","phoneNumber","status","createdAt","roles"]'::jsonb, '{"access_level":"customer_service","can_modify":["status"],"dynamic_filtering":true}'::jsonb
FROM roles r
WHERE r.name = 'EMPLOYEE' ON CONFLICT (role_id, resource_type) DO NOTHING;

-- MANAGER field access (management operations)
INSERT INTO field_access (role_id, resource_type, allowed_fields, config)
SELECT r.role_id,
       'user',
       '["id","username","email","firstName","lastName","phoneNumber","status","roles","createdAt","lastLoginAt","accountType"]'::jsonb, '{"access_level":"management","can_modify":["status","roles"],"dynamic_filtering":true}'::jsonb
FROM roles r
WHERE r.name = 'MANAGER' ON CONFLICT (role_id, resource_type) DO NOTHING;

-- CUSTOMER_SUPPORT field access (support operations)
INSERT INTO field_access (role_id, resource_type, allowed_fields, config)
SELECT r.role_id,
       'user',
       '["id","username","email","firstName","lastName","phoneNumber","status","createdAt"]'::jsonb, '{"access_level":"support","can_modify":[],"dynamic_filtering":true}'::jsonb
FROM roles r
WHERE r.name = 'CUSTOMER_SUPPORT' ON CONFLICT (role_id, resource_type) DO NOTHING;

-- AUDITOR field access (audit and compliance)
INSERT INTO field_access (role_id, resource_type, allowed_fields, config)
SELECT r.role_id,
       'user',
       '["*"]'::jsonb, '{"access_level":"audit","can_modify":[],"dynamic_filtering":true,"audit_trail":true}'::jsonb
FROM roles r
WHERE r.name = 'AUDITOR' ON CONFLICT (role_id, resource_type) DO NOTHING;

-- COMPLIANCE_OFFICER field access (compliance monitoring)
INSERT INTO field_access (role_id, resource_type, allowed_fields, config)
SELECT r.role_id,
       'user',
       '["*"]'::jsonb, '{"access_level":"compliance","can_modify":[],"dynamic_filtering":true,"audit_trail":true}'::jsonb
FROM roles r
WHERE r.name = 'COMPLIANCE_OFFICER' ON CONFLICT (role_id, resource_type) DO NOTHING;

-- Add payment field access for administrative roles
-- ADMIN payment field access (full access)
INSERT INTO field_access (role_id, resource_type, allowed_fields, config)
SELECT r.role_id, 'payment', '["*"]'::jsonb, '{"access_level": "full", "can_modify": true}'::jsonb
FROM roles r
WHERE r.name = 'ADMIN' ON CONFLICT (role_id, resource_type) DO NOTHING;

-- EMPLOYEE payment field access (customer service)
INSERT INTO field_access (role_id, resource_type, allowed_fields, config)
SELECT r.role_id,
       'payment',
       '["paymentId","fromAccount","toAccount","amount","currency","status","createdAt","description","reference"]'::jsonb, '{"access_level":"customer_service","can_modify":["status"],"dynamic_filtering":true}'::jsonb
FROM roles r
WHERE r.name = 'EMPLOYEE' ON CONFLICT (role_id, resource_type) DO NOTHING;

-- MANAGER payment field access (management operations)
INSERT INTO field_access (role_id, resource_type, allowed_fields, config)
SELECT r.role_id,
       'payment',
       '["*"]'::jsonb, '{"access_level":"management","can_modify":["status","description"],"dynamic_filtering":true,"approval_rights":true}'::jsonb
FROM roles r
WHERE r.name = 'MANAGER' ON CONFLICT (role_id, resource_type) DO NOTHING;

-- AUDITOR payment field access (audit trail)
INSERT INTO field_access (role_id, resource_type, allowed_fields, config)
SELECT r.role_id,
       'payment',
       '["*"]'::jsonb, '{"access_level":"audit","can_modify":[],"dynamic_filtering":true,"audit_trail":true,"full_history":true}'::jsonb
FROM roles r
WHERE r.name = 'AUDITOR' ON CONFLICT (role_id, resource_type) DO NOTHING;

-- COMPLIANCE_OFFICER payment field access (compliance monitoring)
INSERT INTO field_access (role_id, resource_type, allowed_fields, config)
SELECT r.role_id,
       'payment',
       '["*"]'::jsonb, '{"access_level":"compliance","can_modify":[],"dynamic_filtering":true,"audit_trail":true,"risk_analysis":true}'::jsonb
FROM roles r
WHERE r.name = 'COMPLIANCE_OFFICER' ON CONFLICT (role_id, resource_type) DO NOTHING;
