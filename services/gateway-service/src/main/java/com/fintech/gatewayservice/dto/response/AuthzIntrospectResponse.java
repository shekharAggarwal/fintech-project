package com.fintech.gatewayservice.dto.response;

import java.util.List;
import java.util.Map;

public class AuthzIntrospectResponse {
    public boolean allowed;
    public String userId;
    public List<String> roles;
    public Map<String, Object> permissions; // e.g. perTxnMax, dailyMax
    public Map<String, List<String>> fieldAccess; // resource -> fields
    public String policyVersion;
    public Integer cacheTtlSeconds; // suggested cache TTL
    public String reason;
    public AuthzIntrospectResponse() {}
}