package org.tavall.couriers.web.view.controller.dsahboard.superuser;


import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.tavall.couriers.api.web.endpoints.Routes;
import org.tavall.couriers.api.web.service.shipping.ShippingLabelMetaDataService;
import org.tavall.couriers.api.web.service.user.UserAccountService;
import org.tavall.couriers.api.web.user.UserAccountEntity;
import org.tavall.couriers.api.web.user.permission.Role;

import java.util.List;
import java.util.Locale;
import java.util.Set;

@Controller
public class SuperuserDashboardController {

    private final ShippingLabelMetaDataService shippingService;
    private final UserAccountService userAccountService;

    public SuperuserDashboardController(ShippingLabelMetaDataService shippingService,
                                        UserAccountService userAccountService) {
        this.shippingService = shippingService;
        this.userAccountService = userAccountService;
    }

    @GetMapping(Routes.SUPERUSER_DASHBOARD)
    public String dashboard(Model model) {
        model.addAttribute("title", "System Administration");
        List<AdminUserView> users = userAccountService.getAllUsers()
                .stream()
                .map(this::toAdminUserView)
                .toList();
        model.addAttribute("adminUsers", users);
        model.addAttribute("allRoles", Role.values());
        model.addAttribute("systemHealth", "98.2%");
        model.addAttribute("totalShipments", shippingService.getAllShipmentLabels().size());

        return "dashboard/superuser/admin-dashboard";
    }

    @PostMapping(Routes.SUPERUSER_DASHBOARD)
    @PreAuthorize("hasRole('SUPERUSER')")
    public String addUser(@RequestParam("username") String username,
                          @RequestParam("role") String role,
                          RedirectAttributes redirectAttributes) {
        try {
            Role selectedRole = Role.valueOf(role.toUpperCase(Locale.ROOT));
            UserAccountEntity created = userAccountService.createUser(username, Set.of(selectedRole));
            redirectAttributes.addFlashAttribute("addStatus", "Created user " + created.getUsername());
        } catch (IllegalArgumentException ex) {
            redirectAttributes.addFlashAttribute("addError", ex.getMessage());
        }
        return "redirect:" + Routes.superUserDashboard();
    }

    private AdminUserView toAdminUserView(UserAccountEntity account) {
        String roleLabel = account.getRoles() == null || account.getRoles().isEmpty()
                ? "USER"
                : account.getRoles().stream()
                .map(Enum::name)
                .sorted()
                .reduce((left, right) -> left + ", " + right)
                .orElse("USER");
        return new AdminUserView(account.getUsername(), roleLabel);
    }

    public record AdminUserView(String name, String role) { }
}
