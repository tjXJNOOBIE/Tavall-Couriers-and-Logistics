package org.tavall.couriers.web.view.controller.dsahboard.driver;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.tavall.couriers.api.delivery.DeliveryStateManager;
import org.tavall.couriers.api.delivery.state.DeliveryState;
import org.tavall.couriers.api.web.entities.ShippingLabelMetaDataEntity;
import org.tavall.couriers.api.web.endpoints.Routes;
import org.tavall.couriers.api.web.service.shipping.ShippingLabelMetaDataService;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Set;

@Controller
public class DriverDashboardController {

    private final ShippingLabelMetaDataService shippingService;

    public DriverDashboardController(ShippingLabelMetaDataService shippingService) {
        this.shippingService = shippingService;
    }

    @GetMapping(Routes.DRIVER_DASHBOARD)
    public String dashboard(Model model) {
        List<ShippingLabelMetaDataEntity> labels = shippingService.getAllShipmentLabels();
        List<ShippingLabelMetaDataEntity> readyLabels = labels.stream()
                .filter(label -> label.getDeliveryState() == DeliveryState.LABEL_CREATED)
                .toList();

        model.addAttribute("title", "Driver Dashboard");
        model.addAttribute("labels", labels);
        model.addAttribute("readyLabels", readyLabels);
        model.addAttribute("hasReadyLabels", !readyLabels.isEmpty());
        model.addAttribute("firstReadyLabel", readyLabels.isEmpty() ? null : readyLabels.get(0));

        return "dashboard/driver/driver-dashboard";
    }

    @GetMapping(Routes.DRIVER_CREATE_LABEL_PAGE)
    public String createLabelPage(Model model,
                                  @RequestParam(value = "created", required = false) String createdUuid) {
        model.addAttribute("title", "Create Label");
        model.addAttribute("shipment", new ShippingLabelMetaDataEntity());

        if (createdUuid != null && !createdUuid.isBlank()) {
            ShippingLabelMetaDataEntity created = shippingService.findByUuid(createdUuid);
            if (created != null) {
                model.addAttribute("createdLabel", created);
            }
        }
        return "dashboard/driver/create-shipment";
    }

    @PostMapping(Routes.DRIVER_CREATE_LABEL)
    public String createLabel(ShippingLabelMetaDataEntity shipment,
                              @RequestParam(value = "deliverByDate", required = false)
                              @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate deliverByDate,
                              RedirectAttributes redirectAttributes) {
        if (deliverByDate != null) {
            shipment.setDeliverBy(deliverByDate.atStartOfDay(ZoneId.systemDefault()).toInstant());
        }

        ShippingLabelMetaDataEntity created = shippingService.createShipment(shipment, DeliveryState.LABEL_CREATED);
        redirectAttributes.addAttribute("created", created.getUuid());
        return "redirect:" + Routes.driverCreateLabelPage();
    }

    @GetMapping(Routes.DRIVER_SCAN_PAGE)
    public String scanPage(Model model,
                           @RequestParam(value = "uuid", required = false) String uuid,
                           @RequestParam(value = "status", required = false) String status,
                           @RequestParam(value = "error", required = false) String error) {
        ShippingLabelMetaDataEntity selected = resolveLabel(uuid);
        DeliveryState currentState = selected != null && selected.getDeliveryState() != null
                ? selected.getDeliveryState()
                : DeliveryState.LABEL_CREATED;

        DeliveryStateManager manager = new DeliveryStateManager(currentState);
        Set<DeliveryState> allowed = manager.getAllowedNextStates();

        model.addAttribute("title", "Driver Scan");
        model.addAttribute("selectedLabel", selected);
        model.addAttribute("allowedTransitions", allowed);
        model.addAttribute("allStates", Arrays.asList(DeliveryState.values()));
        model.addAttribute("transitionStatus", status);
        model.addAttribute("transitionError", error);

        return "dashboard/driver/driver-scan";
    }

    @PostMapping(Routes.DRIVER_TRANSITION_PACKAGE)
    public String transitionPackage(@RequestParam("uuid") String uuid,
                                    @RequestParam("nextState") String nextState,
                                    RedirectAttributes redirectAttributes) {
        ShippingLabelMetaDataEntity label = resolveLabel(uuid);
        if (label == null) {
            redirectAttributes.addAttribute("error", "Label not found.");
            return "redirect:" + Routes.driverScanPage();
        }

        DeliveryState currentState = label.getDeliveryState() != null
                ? label.getDeliveryState()
                : DeliveryState.LABEL_CREATED;

        DeliveryState targetState;
        try {
            targetState = DeliveryState.valueOf(nextState.toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ex) {
            redirectAttributes.addAttribute("error", "Unknown delivery state.");
            redirectAttributes.addAttribute("uuid", label.getUuid());
            return "redirect:" + Routes.driverScanPage();
        }

        DeliveryStateManager manager = new DeliveryStateManager(currentState);
        if (!manager.canTransitionTo(targetState)) {
            redirectAttributes.addAttribute("error", "Transition not allowed from " + currentState + " to " + targetState + ".");
            redirectAttributes.addAttribute("uuid", label.getUuid());
            return "redirect:" + Routes.driverScanPage();
        }

        ShippingLabelMetaDataEntity updated = shippingService.updateDeliveryState(label.getUuid(), targetState);
        if (updated == null) {
            redirectAttributes.addAttribute("error", "Unable to update label state.");
            redirectAttributes.addAttribute("uuid", label.getUuid());
            return "redirect:" + Routes.driverScanPage();
        }

        redirectAttributes.addAttribute("status", "Transitioned to " + targetState + ".");
        redirectAttributes.addAttribute("uuid", label.getUuid());
        return "redirect:" + Routes.driverScanPage();
    }

    private ShippingLabelMetaDataEntity resolveLabel(String uuid) {
        if (uuid != null && !uuid.isBlank()) {
            return shippingService.findByUuid(uuid);
        }
        return shippingService.getAllShipmentLabels().stream().findFirst().orElse(null);
    }
}
