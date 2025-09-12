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

-- BUSINESS_ACCOUNT_HOLDER field access (enhanced business features)
INSERT INTO field_access (role_id, resource_type, allowed_fields, config)
SELECT r.role_id,
       'user',
       '["id","username","email","firstName","lastName","phoneNumber","status","accountType","tier","createdAt","lastLoginAt"]'::jsonb, '{"access_level":"enhanced_self","can_modify":["email","phoneNumber"],"dynamic_filtering":true}'::jsonb
FROM roles r
WHERE r.name = 'BUSINESS_ACCOUNT_HOLDER' ON CONFLICT (role_id, resource_type) DO NOTHING;

-- PREMIUM_ACCOUNT_HOLDER field access (premium features)
INSERT INTO field_access (role_id, resource_type, allowed_fields, config)
SELECT r.role_id,
       'user',
       '["id","username","email","firstName","lastName","phoneNumber","status","accountType","tier","createdAt","lastLoginAt"]'::jsonb, '{"access_level":"premium_self","can_modify":["email","phoneNumber"],"dynamic_filtering":true}'::jsonb
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
