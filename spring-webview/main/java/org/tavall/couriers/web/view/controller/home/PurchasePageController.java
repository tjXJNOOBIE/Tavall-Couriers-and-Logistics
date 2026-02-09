package org.tavall.couriers.web.view.controller.home;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.tavall.couriers.api.web.endpoints.Routes;
import org.tavall.couriers.web.view.controller.home.helper.PurchasePageControllerHelper;

import java.time.LocalDateTime;

@Controller
public class PurchasePageController {

    private final PurchasePageControllerHelper helper;

    public PurchasePageController(PurchasePageControllerHelper helper) {
        this.helper = helper;
    }

    @GetMapping(Routes.PURCHASE)
    @PreAuthorize("hasAnyRole('MERCHANT','DRIVER','SUPERUSER')")
    public String purchasePage(Model model) {
        return helper.renderPurchasePage(model);
    }

    @PostMapping(Routes.PURCHASE)
    @PreAuthorize("hasAnyRole('MERCHANT','DRIVER','SUPERUSER')")
    public String submitPurchase(Model model,
                                 @RequestParam("customerName") String customerName,
                                 @RequestParam(value = "customerPhone", required = false) String customerPhone,
                                 @RequestParam("address") String address,
                                 @RequestParam("city") String city,
                                 @RequestParam("state") String state,
                                 @RequestParam("zip") String zip,
                                 @RequestParam(value = "country", required = false) String country,
                                 @RequestParam("itemName") String itemName,
                                 @RequestParam(value = "quantity", required = false) Integer quantity,
                                 @RequestParam(value = "deliverBy", required = false)
                                 @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime deliverBy) {
        return helper.handlePurchase(model, customerName, customerPhone, address, city, state, zip,
                country, itemName, quantity, deliverBy);
    }
}
