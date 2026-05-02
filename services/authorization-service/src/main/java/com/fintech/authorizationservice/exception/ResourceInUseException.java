package com.fintech.authorizationservice.exception;

public class ResourceInUseException extends RuntimeException {

    private final String resourceName;
    private final Object resourceId;
    private final String reason;

    public ResourceInUseException(String resourceName, Object resourceId, String reason) {
        super(String.format("%s with id '%s' is currently in use: %s", resourceName, resourceId, reason));
        this.resourceName = resourceName;
        this.resourceId = resourceId;
        this.reason = reason;
    }

    public String getResourceName() {
        return resourceName;
    }

    public Object getResourceId() {
        return resourceId;
    }

    public String getReason() {
        return reason;
    }
}
