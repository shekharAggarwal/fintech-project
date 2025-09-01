package com.fintech.gatewayservice.dto.response;

import java.util.List;
import java.util.Map;

public class AuthzIntrospectResponse {
    public boolean allowed;
    public String userId;
    public String role;
    public List<String> permissions;
    public Map<String, Object> limits; // perTxnMax, dailyMax etc
    public Map<String, List<String>> fieldAccess; // resource -> fields
    public String reason;

    public AuthzIntrospectResponse() {
    }
}