package org.tavall.couriers.web.view.controller.tracking;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.tavall.couriers.api.web.entities.ShippingLabelMetaDataEntity;
import org.tavall.couriers.api.web.endpoints.Routes;
import org.tavall.couriers.api.web.service.shipping.ShippingLabelMetaDataService;
import org.tavall.couriers.api.delivery.state.DeliveryState;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.stream.Collectors;

@Controller
public class TrackingPageController {

    private final ShippingLabelMetaDataService shippingService;
    private static final DateTimeFormatter DISPLAY_FORMAT =
            DateTimeFormatter.ofPattern("MM/dd/yyyy hh:mm a").withZone(ZoneId.systemDefault());

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
        Map<String, String> batchStatusMessages = new HashMap<>();
        if (batch != null && !batch.isBlank()) {
            List<String> numbers = Arrays.stream(batch.split("[\\s,]+"))
                    .map(this::normalize)
                    .filter(val -> val != null && !val.isBlank())
                    .distinct()
                    .collect(Collectors.toList());
            batchResults = shippingService.findByTrackingNumbers(numbers);
            for (ShippingLabelMetaDataEntity label : batchResults) {
                if (label != null && label.getTrackingNumber() != null) {
                    batchStatusMessages.put(label.getTrackingNumber(), formatStatusMessage(label));
                }
            }
        }

        model.addAttribute("title", "Track Package");
        model.addAttribute("trackingNumber", normalized);
        model.addAttribute("trackingResult", result);
        model.addAttribute("trackingStatusMessage", formatStatusMessage(result));
        model.addAttribute("batchResults", batchResults);
        model.addAttribute("batchStatusMessages", batchStatusMessages);
        return "tracking";
    }

    private String normalize(String trackingNumber) {
        if (trackingNumber == null || trackingNumber.isBlank()) {
            return null;
        }
        return trackingNumber.trim().toUpperCase(Locale.ROOT);
    }

    private String formatStatusMessage(ShippingLabelMetaDataEntity label) {
        if (label == null || label.getDeliveryState() == null) {
            return "Status is unavailable.";
        }
        DeliveryState state = label.getDeliveryState();
        Instant stamp = label.getDeliverBy() != null ? label.getDeliverBy() : Instant.now();
        String formatted = DISPLAY_FORMAT.format(stamp);
        return switch (state) {
            case LABEL_CREATED -> "Label has been created at " + formatted + ".";
            case IN_HQ -> "Package checked into Tavall HQ at " + formatted + ".";
            case IN_MIDDLEMAN -> "Package arrived at the partner hub at " + formatted + ".";
            case IN_TRANSIT -> "Package departed and is in transit as of " + formatted + ".";
            case OUT_FOR_DELIVERY -> "Out for delivery as of " + formatted + ".";
            case DELIVERED -> "Delivered at " + formatted + ".";
            case ON_HOLD -> "Shipment placed on hold at " + formatted + ".";
            case RETRY -> "Delivery attempt missed; retry scheduled at " + formatted + ".";
            case CANCELLED -> "Shipment was cancelled at " + formatted + ".";
        };
    }
}
