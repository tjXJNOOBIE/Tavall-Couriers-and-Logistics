package org.tavall.couriers.web.view.controller.support;

import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.tavall.couriers.api.web.user.permission.Role;

import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;

@ControllerAdvice
public class ViewModelAdvice {

    private static final List<Role> ROLE_PRIORITY = List.of(
            Role.SUPERUSER,
            Role.MERCHANT,
            Role.DRIVER,
            Role.SUPPORT,
            Role.USER,
            Role.CUSTOMER,
            Role.SYSTEM
    );

    @ModelAttribute
    public void addUserContext(Model model, Authentication authentication) {
        String userName = "Guest";
        String roleName = "GUEST";

        if (authentication != null
                && authentication.isAuthenticated()
                && !(authentication instanceof AnonymousAuthenticationToken)) {
            userName = authentication.getName();
            roleName = resolvePrimaryRole(authentication);
        }

        model.addAttribute("headerUserName", userName);
        model.addAttribute("headerUserRole", roleName);
        model.addAttribute("currentUserRole", roleName);
        model.addAttribute("currentUserName", userName);
        model.addAttribute("isAdminUser", isAdminRole(roleName));
    }

    private String resolvePrimaryRole(Authentication authentication) {
        Set<String> roles = authentication.getAuthorities()
                .stream()
                .map(GrantedAuthority::getAuthority)
                .filter(auth -> auth.startsWith(Role.PREFIX))
                .collect(Collectors.toSet());

        return ROLE_PRIORITY.stream()
                .map(Role::name)
                .filter(role -> roles.contains(Role.PREFIX + role))
                .findFirst()
                .orElse("USER");
    }

    private boolean isAdminRole(String roleName) {
        if (roleName == null) {
            return false;
        }
        String normalized = roleName.trim().toUpperCase(Locale.ROOT);
        return "SUPERUSER".equals(normalized) || "MERCHANT".equals(normalized);
    }
}
