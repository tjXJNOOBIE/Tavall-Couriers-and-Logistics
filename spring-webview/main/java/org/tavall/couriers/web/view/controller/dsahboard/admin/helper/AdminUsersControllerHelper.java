package org.tavall.couriers.web.view.controller.dsahboard.admin.helper;

import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;
import org.springframework.ui.Model;
import org.tavall.couriers.api.web.service.user.UserAccountService;
import org.tavall.couriers.api.web.user.UserAccountEntity;
import org.tavall.couriers.api.web.user.permission.Role;
import org.tavall.couriers.api.web.user.permission.UserPermissions;
import org.tavall.couriers.web.view.controller.dsahboard.admin.model.AdminUserView;

import java.util.List;
import java.util.Set;

@Component
public class AdminUsersControllerHelper {

    private static final List<Role> ROLE_PRIORITY = List.of(
            Role.SUPERUSER,
            Role.MERCHANT,
            Role.DRIVER,
            Role.SUPPORT,
            Role.USER,
            Role.CUSTOMER,
            Role.SYSTEM
    );

    private final UserAccountService userAccountService;

    public AdminUsersControllerHelper(UserAccountService userAccountService) {
        this.userAccountService = userAccountService;
    }

    public String render(Model model, Authentication authentication) {
        List<AdminUserView> adminUsers = userAccountService.getAllUsers()
                .stream()
                .map(this::toView)
                .toList();
        model.addAttribute("title", "User Management");
        model.addAttribute("adminUsers", adminUsers);
        model.addAttribute("canPromote", hasPermission(authentication, UserPermissions.USER_PROMOTE_TO_DRIVER));
        model.addAttribute("canDemote", hasPermission(authentication, UserPermissions.USER_DEMOTE_FROM_DRIVER));
        return "dashboard/admin/admin-users";
    }

    private boolean hasPermission(Authentication authentication, UserPermissions permission) {
        if (authentication == null || permission == null) {
            return false;
        }
        return authentication.getAuthorities()
                .stream()
                .anyMatch(auth -> auth.getAuthority().equals(permission.authority()));
    }

    private AdminUserView toView(UserAccountEntity entity) {
        String name = entity != null && entity.getUsername() != null ? entity.getUsername() : "Unknown";
        String role = resolvePrimaryRole(entity != null ? entity.getRoles() : null);
        return new AdminUserView(name, role);
    }

    private String resolvePrimaryRole(Set<Role> roles) {
        if (roles == null || roles.isEmpty()) {
            return "USER";
        }
        return ROLE_PRIORITY.stream()
                .filter(roles::contains)
                .findFirst()
                .map(Role::name)
                .orElse("USER");
    }
}
