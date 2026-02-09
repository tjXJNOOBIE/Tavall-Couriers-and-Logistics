package org.tavall.couriers.web.view.controller.dsahboard.helper;

import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;
import org.springframework.ui.Model;
import org.tavall.couriers.api.web.endpoints.Routes;
import org.tavall.couriers.api.web.user.permission.Role;
import org.tavall.couriers.web.view.controller.dsahboard.model.DemoCredential;

import java.util.List;

@Component
public class DashboardLoginPageControllerHelper {

    public String dashboardHome(Model model, Authentication authentication) {
        if (authentication == null
                || !authentication.isAuthenticated()
                || authentication instanceof AnonymousAuthenticationToken) {
            return "redirect:" + Routes.dashboardLoginHome();
        }

        if (hasRole(authentication, Role.SUPERUSER)) {
            return "redirect:" + Routes.superUserDashboard();
        }
        if (hasRole(authentication, Role.MERCHANT)) {
            return "redirect:" + Routes.merchantDashboard();
        }
        if (hasRole(authentication, Role.DRIVER)) {
            return "redirect:" + Routes.driverDashboard();
        }

        return "redirect:" + Routes.home();
    }

    public String dashboardLoginRedirect(Model model, Authentication authentication) {
        if (authentication != null
                && authentication.isAuthenticated()
                && !(authentication instanceof AnonymousAuthenticationToken)) {
            return dashboardHome(model, authentication);
        }
        return dashboardLogin(model);
    }

    public String dashboardLogin(Model model) {
        model.addAttribute("title", "Dashboard Login");
        model.addAttribute("demoCredentials", List.of(
                new DemoCredential("Driver", "driver", "driver"),
                new DemoCredential("Merchant", "merchant", "merchant"),
                new DemoCredential("Admin", "superuser", "superuser")
        ));
        return "dashboard/dashboard-login";
    }

    public String loginInternal() {
        return "dashboard/login";
    }

    private boolean hasRole(Authentication authentication, Role role) {
        return authentication.getAuthorities()
                .stream()
                .anyMatch(auth -> auth.getAuthority().equals(role.authority()));
    }
}
