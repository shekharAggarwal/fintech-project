package com.fintech.authorizationservice.model;

import java.util.Set;

public enum UserRole {
    ACCOUNT_HOLDER(Set.of(
        "VIEW_ACCOUNT", 
        "VIEW_TRANSACTION", 
        "TRANSFER_MONEY", 
        "VIEW_BALANCE"
    )),
    SALES_PERSON(Set.of(
        "VIEW_ACCOUNT", 
        "VIEW_TRANSACTION", 
        "VIEW_BALANCE", 
        "CREATE_ACCOUNT", 
        "UPDATE_ACCOUNT", 
        "VIEW_CUSTOMER_LIST",
        "SEND_NOTIFICATION"
    )),
    MANAGER(Set.of(
        "VIEW_ACCOUNT", 
        "VIEW_TRANSACTION", 
        "VIEW_BALANCE", 
        "CREATE_ACCOUNT", 
        "UPDATE_ACCOUNT", 
        "DELETE_ACCOUNT",
        "VIEW_CUSTOMER_LIST",
        "SEND_NOTIFICATION",
        "VIEW_REPORTS",
        "MANAGE_USERS",
        "ADMIN_ACCESS"
    ));

    private final Set<String> authorities;

    UserRole(Set<String> authorities) {
        this.authorities = authorities;
    }

    public Set<String> getAuthorities() {
        return authorities;
    }

    public boolean hasAuthority(String authority) {
        return authorities.contains(authority);
    }
}
