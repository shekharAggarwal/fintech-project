package com.fintech.authorizationservice.dto.request.admin;

public class UpdateFieldAccessRequest {

    private String allowedFields;

    private String config;

    public UpdateFieldAccessRequest() {
    }

    public UpdateFieldAccessRequest(String allowedFields, String config) {
        this.allowedFields = allowedFields;
        this.config = config;
    }

    public String getAllowedFields() {
        return allowedFields;
    }

    public void setAllowedFields(String allowedFields) {
        this.allowedFields = allowedFields;
    }

    public String getConfig() {
        return config;
    }

    public void setConfig(String config) {
        this.config = config;
    }
}
