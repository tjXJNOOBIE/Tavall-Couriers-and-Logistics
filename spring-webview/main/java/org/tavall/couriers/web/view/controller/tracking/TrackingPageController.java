package org.tavall.couriers.web.view.controller.tracking;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.tavall.couriers.api.web.endpoints.Routes;
import org.tavall.couriers.web.view.controller.tracking.helper.TrackingPageControllerHelper;

@Controller
public class TrackingPageController {

    private final TrackingPageControllerHelper helper;

    public TrackingPageController(TrackingPageControllerHelper helper) {
        this.helper = helper;
    }

    @GetMapping(Routes.TRACKING_PAGE)
    public String trackingPage(@RequestParam(value = "trackingNumber", required = false) String trackingNumber,
                               @RequestParam(value = "batch", required = false) String batch,
                               Model model) {
        return helper.render(trackingNumber, batch, model);
    }

    @GetMapping(Routes.TRACKING_DETAIL_TEMPLATE)
    public String trackingDetail(@PathVariable("trackingNumber") String trackingNumber, Model model) {
        return helper.render(trackingNumber, null, model);
    }
}
