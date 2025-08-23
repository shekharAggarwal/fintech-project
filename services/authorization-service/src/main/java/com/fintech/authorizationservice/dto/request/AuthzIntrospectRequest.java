package com.fintech.authorizationservice.dto.request;

import java.util.Map;

public record AuthzIntrospectRequest(String jwtToken, String path, String method, Map<String, Object> context) {

}