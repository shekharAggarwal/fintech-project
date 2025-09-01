-- Add PostgreSQL function for optimized path matching
-- This function handles wildcard matching for authorization paths

CREATE OR REPLACE FUNCTION is_path_matching(requested_path TEXT, permission_path TEXT)
RETURNS BOOLEAN AS $$
BEGIN
    -- Handle null cases
    IF requested_path IS NULL OR permission_path IS NULL THEN
        RETURN FALSE;
    END IF;
    
    -- Exact match
    IF requested_path = permission_path THEN
        RETURN TRUE;
    END IF;
    
    -- Handle /** wildcard suffix (matches any sub-path)
    IF permission_path LIKE '%/**' THEN
        DECLARE
            prefix TEXT := substring(permission_path from 1 for length(permission_path) - 3);
        BEGIN
            RETURN requested_path LIKE prefix || '%';
        END;
    END IF;
    
    -- Handle segment-by-segment wildcard matching
    DECLARE
        requested_segments TEXT[];
        permission_segments TEXT[];
        i INTEGER;
    BEGIN
        -- Split paths into segments
        requested_segments := string_to_array(requested_path, '/');
        permission_segments := string_to_array(permission_path, '/');
        
        -- Different number of segments means no match (unless /** suffix handled above)
        IF array_length(requested_segments, 1) != array_length(permission_segments, 1) THEN
            RETURN FALSE;
        END IF;
        
        -- Check each segment
        FOR i IN 1..array_length(requested_segments, 1) LOOP
            -- Wildcard segment matches anything
            IF permission_segments[i] = '*' THEN
                CONTINUE;
            END IF;
            
            -- Exact segment match required
            IF requested_segments[i] != permission_segments[i] THEN
                RETURN FALSE;
            END IF;
        END LOOP;
        
        RETURN TRUE;
    END;
END;
$$ LANGUAGE plpgsql IMMUTABLE;

-- Create indexes to optimize the authorization queries
CREATE INDEX IF NOT EXISTS idx_api_methods_path_method ON api_methods(path, http_method);
CREATE INDEX IF NOT EXISTS idx_role_permissions_composite ON role_permissions(role_id, method_id, allowed);

-- Create a composite index for user role lookups
CREATE INDEX IF NOT EXISTS idx_user_roles_composite ON user_roles(user_id, role_id);

-- Add index for field access optimization
CREATE INDEX IF NOT EXISTS idx_field_access_composite ON field_access(role_id, resource_type);
