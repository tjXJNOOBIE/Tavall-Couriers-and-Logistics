package org.tavall.couriers.web.view.controller.dsahboard;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.tavall.couriers.api.web.endpoints.dashboard.DefaultDashboardEndpoints;
import org.tavall.couriers.api.web.endpoints.user.UserAuthEndpoints;
import org.tavall.couriers.api.web.endpoints.page.PageViewEndpoints;

@Controller
public class DashboardLoginPageController {


    @GetMapping(DefaultDashboardEndpoints.DASHBOARD_HOME_PATH)
    public String dashboardLogin(Model model) {
        model.addAttribute("title", "Dashboard Login");
        return "dashboard/dashboard-login";
    }

    @GetMapping(DefaultDashboardEndpoints.DASHBOARD_LOGIN_HOME_PATH)
    public String dashboardLoginRedirect() {
        // Maps to src/main/resources/templates/login.html
        return "dashboard/dashboard-login";

    }

    @PostMapping(UserAuthEndpoints.LOGIN_PATH)
    public String loginInternal() {
        // Maps to src/main/resources/templates/login.html
        return "login";
    }
}