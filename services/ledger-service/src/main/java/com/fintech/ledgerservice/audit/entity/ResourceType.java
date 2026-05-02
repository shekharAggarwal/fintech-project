package com.fintech.ledgerservice.audit.entity;

/**
 * Enum representing the type of resource affected by the audited action.
 */
public enum ResourceType {
    TRANSACTION,
    LEDGER_ENTRY,
    ACCOUNT,
    PAYMENT,
    USER,
    SESSION,
    CONFIGURATION
}
