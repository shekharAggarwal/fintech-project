package com.fintech.userservice.entity.enums;

/**
 * @deprecated Use {@link KycStatus} instead for document verification status.
 * Kept for backward compatibility during migration.
 */
@Deprecated
public enum DocumentStatus {
    PENDING,
    UNDER_REVIEW,
    APPROVED,
    REJECTED
}
