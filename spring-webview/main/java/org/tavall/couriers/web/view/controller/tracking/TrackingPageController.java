package org.tavall.couriers.web.view.controller.tracking;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.tavall.couriers.api.web.entities.ShippingLabelMetaDataEntity;
import org.tavall.couriers.api.web.endpoints.Routes;
import org.tavall.couriers.api.web.service.shipping.ShippingLabelMetaDataService;

import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

@Controller
public class TrackingPageController {

    private final ShippingLabelMetaDataService shippingService;

    public TrackingPageController(ShippingLabelMetaDataService shippingService) {
        this.shippingService = shippingService;
    }

    @GetMapping(Routes.TRACKING_PAGE)
    public String trackingPage(@RequestParam(value = "trackingNumber", required = false) String trackingNumber,
                               @RequestParam(value = "batch", required = false) String batch,
                               Model model) {
        return render(trackingNumber, batch, model);
    }

    @GetMapping(Routes.TRACKING_DETAIL_TEMPLATE)
    public String trackingDetail(@PathVariable("trackingNumber") String trackingNumber, Model model) {
        return render(trackingNumber, null, model);
    }

    private String render(String trackingNumber, String batch, Model model) {
        String normalized = normalize(trackingNumber);
        ShippingLabelMetaDataEntity result = null;
        if (normalized != null) {
            result = shippingService.findByTrackingNumber(normalized);
        }

        List<ShippingLabelMetaDataEntity> batchResults = List.of();
        if (batch != null && !batch.isBlank()) {
            List<String> numbers = Arrays.stream(batch.split("[\\s,]+"))
                    .map(this::normalize)
                    .filter(val -> val != null && !val.isBlank())
                    .distinct()
                    .collect(Collectors.toList());
            batchResults = shippingService.findByTrackingNumbers(numbers);
        }

        model.addAttribute("title", "Track Package");
        model.addAttribute("trackingNumber", normalized);
        model.addAttribute("trackingResult", result);
        model.addAttribute("batchResults", batchResults);
        return "tracking";
    }

    private String normalize(String trackingNumber) {
        if (trackingNumber == null || trackingNumber.isBlank()) {
            return null;
        }
        return trackingNumber.trim().toUpperCase(Locale.ROOT);
    }
}
