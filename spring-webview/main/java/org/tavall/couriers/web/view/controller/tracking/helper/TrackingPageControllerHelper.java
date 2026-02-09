package org.tavall.couriers.web.view.controller.tracking.helper;

import org.springframework.stereotype.Component;
import org.springframework.ui.Model;
import org.tavall.couriers.api.delivery.state.DeliveryState;
import org.tavall.couriers.api.web.entities.ShippingLabelMetaDataEntity;
import org.tavall.couriers.api.web.service.shipping.ShippingLabelMetaDataService;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

@Component
public class TrackingPageControllerHelper {

    private static final DateTimeFormatter DISPLAY_FORMAT =
            DateTimeFormatter.ofPattern("MM/dd/yyyy hh:mm a").withZone(ZoneId.systemDefault());
    private final ShippingLabelMetaDataService shippingService;

    public TrackingPageControllerHelper(ShippingLabelMetaDataService shippingService) {
        this.shippingService = shippingService;
    }

    public String render(String trackingNumber, String batch, Model model) {
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
