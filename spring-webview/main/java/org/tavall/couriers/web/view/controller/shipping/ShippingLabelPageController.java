package org.tavall.couriers.web.view.controller.shipping;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.tavall.couriers.api.shipping.database.entities.ShippingLabelMetaDataEntity;
import org.tavall.couriers.api.web.endpoints.shipping.ShippingLabelEndpoints;
import org.tavall.couriers.api.web.service.shipping.ShippingLabelMetaDataService;

import java.time.Instant;
import java.util.List;

@Controller
public class ShippingLabelPageController {

    private final ShippingLabelMetaDataService shippingService;

    public ShippingLabelPageController(ShippingLabelMetaDataService shippingService) {
        this.shippingService = shippingService;
    }

    @GetMapping(ShippingLabelEndpoints.SHIPPING_LABELS_PATH)
    public String list(Model model) {
        return render(model, null);
    }

    @GetMapping(ShippingLabelEndpoints.SHIPPING_LABEL_DETAIL_PATH)
    public String detail(@PathVariable("uuid") String uuid, Model model) {
        return render(model, uuid);
    }

    private String render(Model model, String uuid) {
        List<ShippingLabelMetaDataEntity> labels = shippingService.getAllShipmentLabels();

        ShippingLabelMetaDataEntity selected = null;
        if (uuid != null && !uuid.isBlank()) {
            for (ShippingLabelMetaDataEntity l : labels) {
                if (uuid.equals(l.getUuid())) {
                    selected = l;
                    break;
                }
            }
        }

        model.addAttribute("title", "Shipping Labels");
        model.addAttribute("renderedAt", Instant.now().toString());
        model.addAttribute("labels", labels);
        model.addAttribute("selected", selected);
        model.addAttribute("selectedUuid", uuid);

        return "shipping-labels";
    }
}