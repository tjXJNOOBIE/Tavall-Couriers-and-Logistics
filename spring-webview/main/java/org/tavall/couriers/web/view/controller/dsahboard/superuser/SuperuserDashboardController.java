package org.tavall.couriers.web.view.controller.dsahboard.superuser;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.tavall.couriers.api.web.endpoints.Routes;
import org.tavall.couriers.web.view.controller.dsahboard.superuser.helper.SuperuserDashboardControllerHelper;

@Controller
@PreAuthorize("hasRole('SUPERUSER')")
public class SuperuserDashboardController {

    private final SuperuserDashboardControllerHelper helper;

    public SuperuserDashboardController(SuperuserDashboardControllerHelper helper) {
        this.helper = helper;
    }

    @GetMapping(Routes.SUPERUSER_DASHBOARD)
    public String dashboard(Model model) {
        return helper.dashboard(model);
    }

    @PostMapping(Routes.SUPERUSER_DASHBOARD)
    public String addUser(@RequestParam("username") String username,
                          @RequestParam("role") String role,
                          RedirectAttributes redirectAttributes) {
        return helper.addUser(username, role, redirectAttributes);
    }
}
