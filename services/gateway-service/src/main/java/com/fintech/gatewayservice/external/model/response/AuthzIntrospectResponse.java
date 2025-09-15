package com.fintech.gatewayservice.external.model.response;

import java.util.List;
import java.util.Map;

public class AuthzIntrospectResponse {
    public boolean allowed;
    public String userId;
    public String role;
    public List<String> permissions;
    public Map<String, Map<String, Object>> resourceAccess;
    public String reason;

    public AuthzIntrospectResponse() {
    }
}
