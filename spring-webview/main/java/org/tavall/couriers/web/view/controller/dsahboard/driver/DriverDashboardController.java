package org.tavall.couriers.web.view.controller.dsahboard.driver;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.tavall.couriers.api.web.endpoints.dashboard.driver.DriverDashboardEndpoints;

import java.util.List;
import java.util.Map;

@Controller
public class DriverDashboardController {

    @GetMapping({DriverDashboardEndpoints.DASHBOARD_PATH, DriverDashboardEndpoints.CHECK_LABEL_AVAILABILITY_PATH})
    public String dashboard(Model model) {
        // 1. Mock the User for the Navbar
        // In a real app, this comes from @AuthenticationPrincipal
        model.addAttribute("user", Map.of(
                "username", "Driver Dave",
                "role", "DRIVER"
        ));

        // 2. Mock Recent Scans for the list
        //TODO: Replace with real data from the database/cache
        var scans = List.of(
                Map.of("trackingNumber", "TAVALL-8821", "status", "PICKED_UP", "timestamp", "09:15 AM"),
                Map.of("trackingNumber", "TAVALL-9923", "status", "IN_TRANSIT", "timestamp", "10:30 AM"),
                Map.of("trackingNumber", "TAVALL-1102", "status", "DELIVERED", "timestamp", "11:45 AM")
        );
        model.addAttribute("recentScans", scans);

        // Maps to src/main/resources/templates/dashboard/driver.html
        return "dashboard/driver";
    }

}