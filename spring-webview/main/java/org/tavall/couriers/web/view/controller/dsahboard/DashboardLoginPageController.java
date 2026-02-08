package org.tavall.couriers.web.view.controller.dsahboard;

import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.tavall.couriers.api.web.endpoints.Routes;
import org.tavall.couriers.api.web.user.permission.Role;

import java.util.List;

@Controller
public class DashboardLoginPageController {

    @GetMapping(Routes.DASHBOARD)
    public String dashboardHome(Model model, Authentication authentication) {
        if (authentication == null
                || !authentication.isAuthenticated()
                || authentication instanceof AnonymousAuthenticationToken) {
            return "redirect:" + Routes.home();
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

        return "redirect:" + Routes.dashboardHomeAlias();
    }

    @GetMapping(Routes.DASHBOARD_LOGIN_HOME)
    public String dashboardLoginRedirect(Model model, Authentication authentication) {
        if (authentication != null
                && authentication.isAuthenticated()
                && !(authentication instanceof AnonymousAuthenticationToken)) {
            return dashboardHome(model, authentication);
        }
        // Maps to src/main/resources/templates/dashboard-login.html
        return dashboardLogin(model);
    }

    @PostMapping(Routes.AUTH_LOGIN)
    public String loginInternal() {
        // Maps to src/main/resources/templates/login.html
        return "dashboard/login";
    }

    private String dashboardLogin(Model model) {
        model.addAttribute("title", "Dashboard Login");
        model.addAttribute("demoCredentials", List.of(
                new DemoCredential("Driver", "driver", "driver"),
                new DemoCredential("Merchant", "merchant", "merchant"),
                new DemoCredential("Admin", "superuser", "superuser")
        ));
        return "dashboard/dashboard-login";
    }

    private boolean hasRole(Authentication authentication, Role role) {
        return authentication.getAuthorities()
                .stream()
                .anyMatch(auth -> auth.getAuthority().equals(role.authority()));
    }

    public record DemoCredential(String label, String username, String password) { }
}
