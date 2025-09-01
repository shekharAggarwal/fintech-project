-- Set up field access permissions for different roles
-- PostgreSQL-safe; simplified approach without helper functions

-- ADMIN field access (full access to all fields)
INSERT INTO field_access (role_id, resource_type, allowed_fields, config)
SELECT r.role_id, 'user', '["*"]'::jsonb, '{"access_level": "full", "can_modify": true}'::jsonb
FROM roles r WHERE r.name = 'ADMIN'
ON CONFLICT (role_id, resource_type) DO NOTHING;

INSERT INTO field_access (role_id, resource_type, allowed_fields, config)
SELECT r.role_id, 'account', '["*"]'::jsonb, '{"access_level": "full", "can_modify": true}'::jsonb
FROM roles r WHERE r.name = 'ADMIN'
ON CONFLICT (role_id, resource_type) DO NOTHING;

-- ACCOUNT_HOLDER field access (own data only)
INSERT INTO field_access (role_id, resource_type, allowed_fields, config)
SELECT r.role_id, 'user',
'["id","username","email","firstName","lastName","phoneNumber"]'::jsonb,
'{"access_level":"self_only","can_modify":["email","phoneNumber"]}'::jsonb
FROM roles r WHERE r.name = 'ACCOUNT_HOLDER'
ON CONFLICT (role_id, resource_type) DO NOTHING;

INSERT INTO field_access (role_id, resource_type, allowed_fields, config)
SELECT r.role_id, 'account',
'["accountNumber","accountType","balance","currency","status"]'::jsonb,
'{"access_level":"self_only","can_modify":[]}'::jsonb
FROM roles r WHERE r.name = 'ACCOUNT_HOLDER'
ON CONFLICT (role_id, resource_type) DO NOTHING;
