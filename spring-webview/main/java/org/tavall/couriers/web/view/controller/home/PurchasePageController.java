package org.tavall.couriers.web.view.controller.home;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.tavall.couriers.api.delivery.state.DeliveryState;
import org.tavall.couriers.api.web.endpoints.Routes;
import org.tavall.couriers.api.web.entities.ShippingLabelMetaDataEntity;
import org.tavall.couriers.api.web.service.shipping.ShippingLabelMetaDataService;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;

@Controller
public class PurchasePageController {

    private final ShippingLabelMetaDataService shippingService;

    public PurchasePageController(ShippingLabelMetaDataService shippingService) {
        this.shippingService = shippingService;
    }

    @GetMapping(Routes.PURCHASE)
    @PreAuthorize("hasAnyRole('MERCHANT','DRIVER')")
    public String purchasePage(Model model) {
        model.addAttribute("title", "Customer Demo");
        return "purchase";
    }

    @PostMapping(Routes.PURCHASE)
    @PreAuthorize("hasAnyRole('MERCHANT','DRIVER')")
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
        ShippingLabelMetaDataEntity request = new ShippingLabelMetaDataEntity();
        request.setRecipientName(customerName);
        request.setPhoneNumber(customerPhone);
        request.setAddress(address);
        request.setCity(city);
        request.setState(state);
        request.setZipCode(zip);
        request.setCountry(country == null || country.isBlank() ? "USA" : country);
        if (deliverBy != null) {
            request.setDeliverBy(deliverBy.atZone(ZoneId.systemDefault()).toInstant());
        }

        ShippingLabelMetaDataEntity created = shippingService.createShipment(request, DeliveryState.LABEL_CREATED);

        model.addAttribute("title", "Customer Demo");
        model.addAttribute("createdLabel", created);
        model.addAttribute("itemName", itemName);
        model.addAttribute("quantity", quantity == null || quantity < 1 ? 1 : quantity);
        model.addAttribute("submittedAt", Instant.now());
        return "purchase";
    }
}
