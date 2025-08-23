package com.fintech.gatewayservice.dto.request;

import java.util.Map;

public class AuthzIntrospectRequest {
    public String jwtToken;
    public String path;
    public String method;
    public Map<String, Object> context;
    public AuthzIntrospectRequest() {}
}