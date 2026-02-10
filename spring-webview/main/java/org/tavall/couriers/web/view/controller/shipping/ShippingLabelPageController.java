package org.tavall.couriers.web.view.controller.shipping;

import org.springframework.core.io.Resource;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.tavall.couriers.api.web.endpoints.Routes;
import org.tavall.couriers.web.view.controller.shipping.helper.ShippingLabelPageControllerHelper;

@Controller
public class ShippingLabelPageController {

    private final ShippingLabelPageControllerHelper helper;

    public ShippingLabelPageController(ShippingLabelPageControllerHelper helper) {
        this.helper = helper;
    }

    @GetMapping(Routes.SHIPPING_LABELS)
    public String list(Model model) {
        return helper.render(model, null);
    }

    @GetMapping(Routes.SHIPPING_LABEL_DETAIL_TEMPLATE)
    public String detail(@PathVariable("uuid") String uuid, Model model) {
        return helper.render(model, uuid);
    }

    @GetMapping(value = Routes.SHIPPING_LABEL_PDF_TEMPLATE, produces = MediaType.APPLICATION_PDF_VALUE)
    public ResponseEntity<Resource> labelPdf(@PathVariable("uuid") String uuid) {
        return helper.labelPdf(uuid);
    }
}
