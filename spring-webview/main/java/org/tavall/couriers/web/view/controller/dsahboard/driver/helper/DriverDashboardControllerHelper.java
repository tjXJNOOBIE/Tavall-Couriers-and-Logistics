package org.tavall.couriers.web.view.controller.dsahboard.driver.helper;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.tavall.couriers.api.delivery.DeliveryStateManager;
import org.tavall.couriers.api.delivery.state.DeliveryState;
import org.tavall.couriers.api.web.entities.DeliveryRouteEntity;
import org.tavall.couriers.api.web.entities.DeliveryRouteStopEntity;
import org.tavall.couriers.api.web.entities.ShippingLabelMetaDataEntity;
import org.tavall.couriers.api.web.endpoints.Routes;
import org.tavall.couriers.api.web.service.route.DeliveryRouteService;
import org.tavall.couriers.api.web.service.shipping.ShippingLabelMetaDataService;
import org.tavall.couriers.api.web.service.user.UserAccountService;
import org.tavall.couriers.api.web.user.UserAccountEntity;
import org.tavall.couriers.web.view.ManualAddressVerificationService;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

@Component
public class DriverDashboardControllerHelper {

    private final ShippingLabelMetaDataService shippingService;
    private final DeliveryRouteService routeService;
    private final UserAccountService userAccountService;
    private final ManualAddressVerificationService addressVerificationService;

    public DriverDashboardControllerHelper(ShippingLabelMetaDataService shippingService,
                                           DeliveryRouteService routeService,
                                           UserAccountService userAccountService,
                                           ManualAddressVerificationService addressVerificationService) {
        this.shippingService = shippingService;
        this.routeService = routeService;
        this.userAccountService = userAccountService;
        this.addressVerificationService = addressVerificationService;
    }

    public String dashboard(Model model,
                            Authentication authentication,
                            @RequestParam(value = "routeId", required = false) String routeId) {
        UUID driverId = resolveDriverId(authentication);
        List<DeliveryRouteEntity> routes = routeService.getAllRoutes();

        DeliveryRouteEntity activeRoute = null;
        if (driverId != null) {
            activeRoute = routes.stream()
                    .filter(route -> route != null && driverId.equals(route.getAssignedDrivers()))
                    .findFirst()
                    .orElse(null);
        }
        if (activeRoute == null && routeId != null && !routeId.isBlank()) {
            activeRoute = routeService.findRoute(routeId);
        }
        List<DeliveryRouteStopEntity> routeStops = activeRoute != null
                ? routeService.getRouteStops(activeRoute.getRouteId())
                : List.of();
        List<ShippingLabelMetaDataEntity> routeLabels = new ArrayList<>();
        for (DeliveryRouteStopEntity stop : routeStops) {
            if (stop == null || stop.getLabelUuid() == null) {
                continue;
            }
            ShippingLabelMetaDataEntity label = shippingService.findByUuid(stop.getLabelUuid());
            if (label != null) {
                routeLabels.add(label);
            }
        }

        List<ShippingLabelMetaDataEntity> preScanLabels = routeLabels.isEmpty() ? List.of() : routeLabels;

        model.addAttribute("title", "Driver Dashboard");
        model.addAttribute("preScanLabels", preScanLabels);
        model.addAttribute("usingRouteQueue", !routeLabels.isEmpty());
        model.addAttribute("preScanRouteId", activeRoute != null ? activeRoute.getRouteId() : null);
        model.addAttribute("availableRoutes", routes);

        return "dashboard/driver/driver-dashboard";
    }

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

    public String createLabel(ShippingLabelMetaDataEntity shipment,
                              @RequestParam(value = "deliverByDate", required = false)
                              @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate deliverByDate,
                              Model model,
                              RedirectAttributes redirectAttributes) {
        if (deliverByDate != null) {
            shipment.setDeliverBy(deliverByDate.atStartOfDay(ZoneId.systemDefault()).toInstant());
        }

        if (!addressVerificationService.isKnownAddress(shipment)) {
            model.addAttribute("title", "Create Label");
            model.addAttribute("shipment", shipment);
            model.addAttribute("addressError", "Address could not be verified. Please review it and try again.");
            model.addAttribute("deliverByDate", deliverByDate);
            return "dashboard/driver/create-shipment";
        }

        ShippingLabelMetaDataEntity created = shippingService.createShipment(shipment, DeliveryState.LABEL_CREATED);
        redirectAttributes.addAttribute("created", created.getUuid());
        return "redirect:" + Routes.driverCreateLabelPage();
    }

    public String scanPage(Model model) {
        model.addAttribute("title", "Driver Scan");
        return "dashboard/driver/driver-scan";
    }

    public String statePage(Model model,
                            @RequestParam(value = "uuid", required = false) String uuid,
                            @RequestParam(value = "status", required = false) String status,
                            @RequestParam(value = "error", required = false) String error) {
        ShippingLabelMetaDataEntity selected = resolveLabel(uuid);
        DeliveryState currentState = selected != null && selected.getDeliveryState() != null
                ? selected.getDeliveryState()
                : DeliveryState.LABEL_CREATED;

        DeliveryStateManager manager = new DeliveryStateManager(currentState);
        Set<DeliveryState> allowed = manager.getAllowedNextStates();

        model.addAttribute("title", "State Change");
        model.addAttribute("selectedLabel", selected);
        model.addAttribute("allowedTransitions", allowed);
        model.addAttribute("allStates", Arrays.asList(DeliveryState.values()));
        model.addAttribute("transitionStatus", status);
        model.addAttribute("transitionError", error);
        return "dashboard/driver/driver-state";
    }

    public String transitionPackage(@RequestParam("uuid") String uuid,
                                    @RequestParam("nextState") String nextState,
                                    RedirectAttributes redirectAttributes) {
        ShippingLabelMetaDataEntity label = resolveLabel(uuid);
        if (label == null) {
            redirectAttributes.addAttribute("error", "Label not found.");
            return "redirect:" + Routes.driverStatePage();
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
            return "redirect:" + Routes.driverStatePage();
        }

        DeliveryStateManager manager = new DeliveryStateManager(currentState);
        if (!manager.canTransitionTo(targetState)) {
            redirectAttributes.addAttribute("error", "Transition not allowed from " + currentState + " to " + targetState + ".");
            redirectAttributes.addAttribute("uuid", label.getUuid());
            return "redirect:" + Routes.driverStatePage();
        }

        ShippingLabelMetaDataEntity updated = shippingService.updateDeliveryState(label.getUuid(), targetState);
        if (updated == null) {
            redirectAttributes.addAttribute("error", "Unable to update label state.");
            redirectAttributes.addAttribute("uuid", label.getUuid());
            return "redirect:" + Routes.driverStatePage();
        }

        redirectAttributes.addAttribute("status", "Transitioned to " + targetState + ".");
        redirectAttributes.addAttribute("uuid", label.getUuid());
        return "redirect:" + Routes.driverStatePage();
    }

    private ShippingLabelMetaDataEntity resolveLabel(String uuid) {
        if (uuid == null || uuid.isBlank()) {
            return null;
        }
        return shippingService.findByUuid(uuid);
    }

    private UUID resolveDriverId(Authentication authentication) {
        if (authentication == null || authentication.getName() == null) {
            return null;
        }
        UserAccountEntity account = userAccountService.findByUsername(authentication.getName());
        return account != null ? account.getUserUUID() : null;
    }
}
