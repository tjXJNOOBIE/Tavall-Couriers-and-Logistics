package org.tavall.couriers.web.view.controller.dsahboard.admin;

import org.springframework.security.core.Authentication;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.tavall.couriers.api.web.endpoints.Routes;
import org.tavall.couriers.web.view.controller.dsahboard.admin.helper.AdminUsersControllerHelper;

@Controller
@PreAuthorize("hasAnyAuthority('PERM_USER_PROMOTE_TO_DRIVER','PERM_USER_DEMOTE_FROM_DRIVER','PERM_ADMIN_VIEW_USERS')")
public class AdminUsersController {

    private final AdminUsersControllerHelper helper;

    public AdminUsersController(AdminUsersControllerHelper helper) {
        this.helper = helper;
    }

    @GetMapping(Routes.DASHBOARD_ADMIN_USERS)
    public String adminUsers(Model model, Authentication authentication) {
        return helper.render(model, authentication);
    }
}
