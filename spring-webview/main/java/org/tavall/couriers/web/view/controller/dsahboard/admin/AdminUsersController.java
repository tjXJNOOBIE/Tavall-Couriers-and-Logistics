package org.tavall.couriers.web.view.controller.dsahboard.admin;

import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.tavall.couriers.api.web.endpoints.Routes;
import org.tavall.couriers.api.web.user.permission.UserPermissions;

import java.util.List;

@Controller
public class AdminUsersController {

    @GetMapping(Routes.DASHBOARD_ADMIN_USERS)
    public String adminUsers(Model model, Authentication authentication) {
        model.addAttribute("title", "User Management");
        model.addAttribute("adminUsers", List.of(
                new AdminUserView("driver", "DRIVER"),
                new AdminUserView("merchant", "MERCHANT"),
                new AdminUserView("superuser", "SUPERUSER"),
                new AdminUserView("user", "USER")
        ));
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

    public record AdminUserView(String name, String role) { }
}
