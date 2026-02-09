package org.tavall.couriers.web.view.controller.home.helper;

import org.springframework.stereotype.Component;
import org.springframework.ui.Model;
import org.tavall.couriers.api.delivery.state.DeliveryState;
import org.tavall.couriers.api.web.entities.ShippingLabelMetaDataEntity;
import org.tavall.couriers.api.web.service.shipping.ShippingLabelMetaDataService;
import org.tavall.couriers.web.view.ManualAddressVerificationService;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

@Component
public class PurchasePageControllerHelper {

    private final ShippingLabelMetaDataService shippingService;
    private final ManualAddressVerificationService addressVerificationService;

    public PurchasePageControllerHelper(ShippingLabelMetaDataService shippingService,
                                        ManualAddressVerificationService addressVerificationService) {
        this.shippingService = shippingService;
        this.addressVerificationService = addressVerificationService;
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
        String resolvedCountry = country == null || country.isBlank() ? "USA" : country;
        request.setCountry(resolvedCountry);
        if (deliverBy != null) {
            request.setDeliverBy(deliverBy.atZone(ZoneId.systemDefault()).toInstant());
        }

        if (!addressVerificationService.isKnownAddress(request)) {
            populateForm(model, customerName, customerPhone, address, city, state, zip, resolvedCountry, itemName, quantity, deliverBy);
            model.addAttribute("addressError", "Address could not be verified. Please review it and try again.");
            return "purchase";
        }

        ShippingLabelMetaDataEntity created = shippingService.createShipment(request, DeliveryState.LABEL_CREATED);

        model.addAttribute("title", "Customer Demo");
        model.addAttribute("createdLabel", created);
        model.addAttribute("itemName", itemName);
        model.addAttribute("quantity", quantity == null || quantity < 1 ? 1 : quantity);
        model.addAttribute("submittedAt", Instant.now());
        return "purchase";
    }

    private void populateForm(Model model,
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
        model.addAttribute("title", "Customer Demo");
        model.addAttribute("customerName", customerName);
        model.addAttribute("customerPhone", customerPhone);
        model.addAttribute("address", address);
        model.addAttribute("city", city);
        model.addAttribute("state", state);
        model.addAttribute("zip", zip);
        model.addAttribute("country", country);
        model.addAttribute("itemName", itemName);
        model.addAttribute("quantity", quantity);
        if (deliverBy != null) {
            String formatted = deliverBy.format(DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm"));
            model.addAttribute("deliverBy", formatted);
        }
    }
}
