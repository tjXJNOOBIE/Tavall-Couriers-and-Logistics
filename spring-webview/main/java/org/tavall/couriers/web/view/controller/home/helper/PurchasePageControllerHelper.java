package org.tavall.couriers.web.view.controller.home.helper;

import org.springframework.stereotype.Component;
import org.springframework.ui.Model;
import org.tavall.couriers.api.delivery.state.DeliveryState;
import org.tavall.couriers.api.web.entities.ShippingLabelMetaDataEntity;
import org.tavall.couriers.api.web.service.shipping.ShippingLabelMetaDataService;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;

@Component
public class PurchasePageControllerHelper {

    private final ShippingLabelMetaDataService shippingService;

    public PurchasePageControllerHelper(ShippingLabelMetaDataService shippingService) {
        this.shippingService = shippingService;
    }

    public String renderPurchasePage(Model model) {
        model.addAttribute("title", "Customer Demo");
        return "purchase";
    }

    public String handlePurchase(Model model,
                                 String customerName,
                                 String customerPhone,
                                 String address,
                                 String city,
                                 String state,
                                 String zip,
                                 String country,
                                 String itemName,
                                 Integer quantity,
                                 LocalDateTime deliverBy) {
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
