package com.portside.trading.service;

import com.portside.trading.domain.Role;
import com.portside.trading.security.CurrentUserService;
import java.util.Map;
import java.util.Set;
import org.springframework.stereotype.Service;

/**
 * Module view/edit matrix, ported 1:1 from the design prototype's ROLE_ACCESS / EDIT_RIGHTS
 * maps so the same business rule (who can see what, who can change what) is enforced here
 * server-side instead of only in the UI.
 */
@Service
public class AccessService {

    private final CurrentUserService currentUser;

    public AccessService(CurrentUserService currentUser) {
        this.currentUser = currentUser;
    }

    private static final Map<Role, Set<String>> VIEW = Map.of(
            Role.ACCOUNTANT, Set.of("dash", "containers", "costing", "suppliers", "purchases", "stock",
                    "lotstock", "stockentries", "items", "sales", "pricing", "customers", "receivables",
                    "receipts", "gl", "tb", "profit", "reports"),
            Role.SALES_OFFICER, Set.of("dash", "stock", "lotstock", "items", "sales", "pricing",
                    "customers", "receivables", "reports"),
            Role.STORE_KEEPER, Set.of("dash", "containers", "purchases", "stock", "lotstock",
                    "stockentries", "items"),
            Role.IMPORT_DESK, Set.of("dash", "containers", "costing", "suppliers", "purchases", "items",
                    "stock", "stockentries", "reports")
    );

    private static final Map<String, Set<Role>> EDIT = Map.ofEntries(
            Map.entry("items", Set.of(Role.OWNER, Role.ACCOUNTANT, Role.IMPORT_DESK, Role.STORE_KEEPER)),
            Map.entry("customers", Set.of(Role.OWNER, Role.ACCOUNTANT, Role.SALES_OFFICER)),
            Map.entry("suppliers", Set.of(Role.OWNER, Role.ACCOUNTANT, Role.IMPORT_DESK)),
            Map.entry("containers", Set.of(Role.OWNER, Role.IMPORT_DESK, Role.ACCOUNTANT)),
            Map.entry("purchases", Set.of(Role.OWNER, Role.IMPORT_DESK, Role.ACCOUNTANT)),
            Map.entry("stockentries", Set.of(Role.OWNER, Role.ACCOUNTANT, Role.STORE_KEEPER, Role.IMPORT_DESK)),
            Map.entry("receipts", Set.of(Role.OWNER, Role.ACCOUNTANT)),
            Map.entry("sales", Set.of(Role.OWNER, Role.ACCOUNTANT, Role.SALES_OFFICER)),
            Map.entry("pricing", Set.of(Role.OWNER, Role.ACCOUNTANT))
    );

    public boolean canView(String module) {
        Role role = currentUser.role();
        if (role == null) return false;
        if (role == Role.OWNER) return true;
        Set<String> allowed = VIEW.get(role);
        return allowed != null && allowed.contains(module);
    }

    public boolean canEdit(String module) {
        Role role = currentUser.role();
        if (role == null) return false;
        if (role == Role.OWNER) return true;
        Set<Role> allowed = EDIT.get(module);
        return allowed != null && allowed.contains(role);
    }

    public boolean isOwner() {
        return currentUser.role() == Role.OWNER;
    }
}
