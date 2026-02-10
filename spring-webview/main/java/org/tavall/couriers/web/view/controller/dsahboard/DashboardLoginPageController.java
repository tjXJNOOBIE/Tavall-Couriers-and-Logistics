package org.tavall.couriers.web.view.controller.dsahboard;

import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.tavall.couriers.api.web.endpoints.Routes;
import org.tavall.couriers.web.view.controller.dsahboard.helper.DashboardLoginPageControllerHelper;
@Controller
public class DashboardLoginPageController {

    private final DashboardLoginPageControllerHelper helper;

    public DashboardLoginPageController(DashboardLoginPageControllerHelper helper) {
        this.helper = helper;
    }

    @GetMapping(Routes.DASHBOARD)
    public String dashboardHome(Model model, Authentication authentication) {
        return helper.dashboardHome(model, authentication);
    }

    @GetMapping(Routes.DASHBOARD_HOME_ALIAS)
    public String dashboardHomeAlias(Model model, Authentication authentication) {
        return helper.dashboardHome(model, authentication);
    }

    @GetMapping(Routes.DASHBOARD_LOGIN_HOME)
    public String dashboardLoginRedirect(Model model, Authentication authentication) {
        return helper.dashboardLoginRedirect(model, authentication);
    }

    @PostMapping(Routes.AUTH_LOGIN)
    public String loginInternal() {
        return helper.loginInternal();
    }
}
