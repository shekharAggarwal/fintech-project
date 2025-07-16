package com.fintech.authorizationservice.service;

import com.fintech.authorizationservice.entity.Permission;
import com.fintech.authorizationservice.entity.Role;
import com.fintech.authorizationservice.entity.RolePermission;
import com.fintech.authorizationservice.repository.RoleRepository;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;

@Service
public class DataInitializationService {

    @Autowired
    private RoleRepository roleRepository;

    @PostConstruct
    @Transactional
    public void initializeData() {
        initializeRolesAndPermissions();
    }

    private void initializeRolesAndPermissions() {
        // Check if data already exists
        if (roleRepository.count() > 0) {
            return; // Data already initialized
        }

        // Create permissions
        List<Permission> permissions = createPermissions();
        
        // Create roles with permissions
        createAccountHolderRole(permissions);
        createSalesPersonRole(permissions);
        createManagerRole(permissions);
    }

    private List<Permission> createPermissions() {
        return Arrays.asList(
            new Permission("ACCOUNT_VIEW", "View account details", "account", "view"),
            new Permission("ACCOUNT_CREATE", "Create new account", "account", "create"),
            new Permission("ACCOUNT_UPDATE", "Update account information", "account", "update"),
            new Permission("ACCOUNT_DELETE", "Delete account", "account", "delete"),
            
            new Permission("TRANSACTION_VIEW", "View transactions", "transaction", "view"),
            new Permission("TRANSACTION_CREATE", "Create transactions/transfers", "transaction", "create"),
            
            new Permission("BALANCE_VIEW", "View account balance", "balance", "view"),
            
            new Permission("CUSTOMERS_VIEW", "View customer list", "customers", "view"),
            
            new Permission("NOTIFICATION_SEND", "Send notifications", "notification", "send"),
            
            new Permission("REPORTS_VIEW", "View reports", "reports", "view"),
            new Permission("REPORTS_GENERATE", "Generate reports", "reports", "generate"),
            
            new Permission("USERS_MANAGE", "Manage users", "users", "manage"),
            
            new Permission("ADMIN_ACCESS", "Admin access", "admin", "access")
        );
    }

    private void createAccountHolderRole(List<Permission> allPermissions) {
        Role accountHolder = new Role("ACCOUNT_HOLDER", "Regular bank account holder");
        
        // Account holder permissions
        List<String> accountHolderPerms = Arrays.asList(
            "ACCOUNT_VIEW", "TRANSACTION_VIEW", "TRANSACTION_CREATE", "BALANCE_VIEW"
        );
        
        accountHolder.setPermissions(new HashSet<>(createRolePermissions(accountHolder, allPermissions, accountHolderPerms)));
        roleRepository.save(accountHolder);
    }

    private void createSalesPersonRole(List<Permission> allPermissions) {
        Role salesPerson = new Role("SALES_PERSON", "Bank sales person");
        
        // Sales person permissions
        List<String> salesPersonPerms = Arrays.asList(
            "ACCOUNT_VIEW", "ACCOUNT_CREATE", "ACCOUNT_UPDATE", 
            "TRANSACTION_VIEW", "BALANCE_VIEW", "CUSTOMERS_VIEW", "NOTIFICATION_SEND"
        );
        
        salesPerson.setPermissions(new HashSet<>(createRolePermissions(salesPerson, allPermissions, salesPersonPerms)));
        roleRepository.save(salesPerson);
    }

    private void createManagerRole(List<Permission> allPermissions) {
        Role manager = new Role("MANAGER", "Bank manager with full access");
        
        // Manager has all permissions
        List<String> managerPerms = allPermissions.stream()
                .map(Permission::getName)
                .toList();
        
        manager.setPermissions(new HashSet<>(createRolePermissions(manager, allPermissions, managerPerms)));
        roleRepository.save(manager);
    }

    private List<RolePermission> createRolePermissions(Role role, List<Permission> allPermissions, List<String> permissionNames) {
        List<RolePermission> rolePermissions = new ArrayList<>();
        
        for (String permName : permissionNames) {
            Permission permission = allPermissions.stream()
                    .filter(p -> p.getName().equals(permName))
                    .findFirst()
                    .orElse(null);
            
            if (permission != null) {
                rolePermissions.add(new RolePermission(role, permission));
            }
        }
        
        return rolePermissions;
    }
}
