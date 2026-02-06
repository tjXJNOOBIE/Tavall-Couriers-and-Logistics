package org.tavall.couriers.web.view.controller;


import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.tavall.couriers.api.web.endpoints.dashboard.superuser.SuperUserEndpoints;

import java.util.Map;

@Controller
public class SuperuserDashboardController {

    @GetMapping({SuperUserEndpoints.DASHBOARD_PATH, SuperUserEndpoints.GET_ALL_SHIPMENTS_PATH})
    public String dashboard(Model model) {
        // 1. Mock User
        model.addAttribute("user", Map.of(
                "username", "Merchant Mary",
                "role", "MERCHANT"));

        // 2. Mock Stats
        model.addAttribute("totalShipments", 142);

        // Maps to src/main/resources/templates/dashboard/merchant.html
        return "dashboard/merchant";
    }

    @PostMapping({SuperUserEndpoints.CREATE_SHIPMENT_PAGE_PATH, SuperUserEndpoints.FORCE_UPDATE_SHIPMENT_PATH})
    public String createShipment() {
        // Logic to save shipment would go here
        // For demo, just redirect back with a success param (optional)
        return "redirect:" + SuperUserEndpoints.GET_ALL_SHIPMENTS_PATH + "?success";
    }

}
