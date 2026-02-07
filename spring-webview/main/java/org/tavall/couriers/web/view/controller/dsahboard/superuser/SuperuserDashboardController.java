package org.tavall.couriers.web.view.controller.dsahboard.superuser;


import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.tavall.couriers.api.web.endpoints.Routes;
import org.tavall.couriers.api.web.service.shipping.ShippingLabelMetaDataService;

import java.util.List;

@Controller
public class SuperuserDashboardController {

    private final ShippingLabelMetaDataService shippingService;

    public SuperuserDashboardController(ShippingLabelMetaDataService shippingService) {
        this.shippingService = shippingService;
    }

    @GetMapping(Routes.SUPERUSER_DASHBOARD)
    public String dashboard(Model model) {
        model.addAttribute("title", "System Administration");
        model.addAttribute("adminUsers", List.of(
                new AdminUserView("driver", "DRIVER"),
                new AdminUserView("merchant", "MERCHANT"),
                new AdminUserView("superuser", "SUPERUSER"),
                new AdminUserView("user", "USER")
        ));
        model.addAttribute("systemHealth", "98.2%");
        model.addAttribute("totalShipments", shippingService.getAllShipmentLabels().size());

        return "dashboard/superuser/admin-dashboard";
    }

    public record AdminUserView(String name, String role) { }
}
