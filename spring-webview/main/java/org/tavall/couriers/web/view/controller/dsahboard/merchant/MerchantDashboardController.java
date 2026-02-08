package org.tavall.couriers.web.view.controller.dsahboard.merchant;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.tavall.couriers.api.delivery.state.DeliveryState;
import org.tavall.couriers.api.qr.scan.cache.ScanCacheService;
import org.tavall.couriers.api.qr.scan.metadata.ScanResponse;
import org.tavall.couriers.api.web.entities.DeliveryRouteEntity;
import org.tavall.couriers.api.web.entities.DeliveryRouteStopEntity;
import org.tavall.couriers.api.web.entities.ShippingLabelMetaDataEntity;
import org.tavall.couriers.api.web.endpoints.Routes;
import org.tavall.couriers.api.web.service.camera.CameraPageService;
import org.tavall.couriers.api.web.service.route.DeliveryRouteService;
import org.tavall.couriers.api.web.service.shipping.ShippingLabelMetaDataService;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Locale;

@Controller
public class MerchantDashboardController {

    private final ShippingLabelMetaDataService shippingService;
    private final DeliveryRouteService routeService;
    private final ScanCacheService scanCache;
    private final CameraPageService cameraPageService;

    public MerchantDashboardController(ShippingLabelMetaDataService shippingService,
                                       DeliveryRouteService routeService,
                                       ScanCacheService scanCache,
                                       CameraPageService cameraPageService) {
        this.shippingService = shippingService;
        this.routeService = routeService;
        this.scanCache = scanCache;
        this.cameraPageService = cameraPageService;
    }

    @GetMapping(Routes.MERCHANT_DASHBOARD)
    public String dashboard(Model model, @RequestParam(value = "created", required = false) String createdUuid) {
        List<ShippingLabelMetaDataEntity> labels = shippingService.getAllShipmentLabels();
        int routeCount = routeService.getAllRoutes().size();
        ShippingLabelMetaDataEntity firstScanError = labels.stream()
                .filter(label -> label.getDeliveryState() == DeliveryState.CANCELLED)
                .findFirst()
                .orElse(null);
        long scanErrorCount = labels.stream()
                .filter(label -> label.getDeliveryState() == DeliveryState.CANCELLED)
                .count();
        List<ScanResponse> scanQueue = scanCache.getRecentResponses(4);
        List<ScanResponse> scanProcessing = cameraPageService.getProcessingResponses();
        model.addAttribute("title", "Merchant Hub");
        model.addAttribute("shipment", new ShippingLabelMetaDataEntity());
        model.addAttribute("allStates", Arrays.asList(DeliveryState.values()));
        model.addAttribute("scanQueue", scanQueue);
        model.addAttribute("scanProcessing", scanProcessing);
        model.addAttribute("scanErrors", labels.stream()
                .filter(label -> label.getDeliveryState() == DeliveryState.CANCELLED)
                .limit(2)
                .toList());
        model.addAttribute("totalShipments", labels.size());
        model.addAttribute("routeCount", routeCount);
        model.addAttribute("scanErrorCount", scanErrorCount);
        model.addAttribute("firstScanErrorUuid", firstScanError != null ? firstScanError.getUuid() : null);

        if (createdUuid != null && !createdUuid.isBlank()) {
            ShippingLabelMetaDataEntity created = shippingService.findByUuid(createdUuid);
            if (created != null) {
                model.addAttribute("createdLabel", created);
            }
        }

        return "dashboard/merchant/merchant-dashboard";
    }

    @GetMapping(Routes.MERCHANT_CREATE_SHIPMENT_PAGE)
    public String createShipmentPage() {
        return "redirect:" + Routes.merchantDashboard();
    }

    @PostMapping(Routes.MERCHANT_CREATE_SHIPMENT)
    public String createShipment(ShippingLabelMetaDataEntity shipment,
                                 @RequestParam(value = "deliverByDate", required = false)
                                 @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate deliverByDate,
                                 @RequestParam(value = "initialState", required = false) String initialState,
                                 RedirectAttributes redirectAttributes) {
        if (deliverByDate != null) {
            shipment.setDeliverBy(deliverByDate.atStartOfDay(ZoneId.systemDefault()).toInstant());
        }

        DeliveryState state = DeliveryState.LABEL_CREATED;
        if (initialState != null && !initialState.isBlank()) {
            try {
                state = DeliveryState.valueOf(initialState.toUpperCase(Locale.ROOT));
            } catch (IllegalArgumentException ignored) {
                state = DeliveryState.LABEL_CREATED;
            }
        }

        ShippingLabelMetaDataEntity created = shippingService.createShipment(shipment, state);
        redirectAttributes.addAttribute("created", created.getUuid());
        return "redirect:" + Routes.merchantDashboard();
    }

    @GetMapping(Routes.MERCHANT_SCAN_PAGE)
    public String scanPage(Model model, @RequestParam(value = "uuid", required = false) String uuid) {
        List<ShippingLabelMetaDataEntity> labels = shippingService.getAllShipmentLabels();
        ShippingLabelMetaDataEntity selected = null;
        if (uuid != null && !uuid.isBlank()) {
            selected = shippingService.findByUuid(uuid);
        }
        model.addAttribute("scanQueue", labels.stream().limit(3).toList());
        model.addAttribute("batchErrors", labels.stream()
                .filter(label -> label.getDeliveryState() == DeliveryState.CANCELLED)
                .limit(2)
                .toList());
        model.addAttribute("batchProgress", Math.min(90, labels.size() * 10));
        model.addAttribute("selectedLabel", selected);
        return "dashboard/merchant/merchant-scan";
    }

    @GetMapping(Routes.MERCHANT_SHIPMENTS_PAGE)
    public String shipmentsPage(Model model,
                                @RequestParam(value = "uuid", required = false) String uuid,
                                @RequestParam(value = "status", required = false) String status,
                                @RequestParam(value = "error", required = false) String error) {
        List<ShippingLabelMetaDataEntity> labels = shippingService.getAllShipmentLabels();
        ShippingLabelMetaDataEntity selected = null;
        if (uuid != null && !uuid.isBlank()) {
            selected = shippingService.findByUuid(uuid);
        }

        model.addAttribute("title", "Shipment Library");
        model.addAttribute("labels", labels);
        model.addAttribute("selectedLabel", selected);
        model.addAttribute("allStates", Arrays.asList(DeliveryState.values()));
        model.addAttribute("updateStatus", status);
        model.addAttribute("updateError", error);
        model.addAttribute("canTransitionAny", true);
        return "dashboard/merchant/merchant-shipments";
    }

    @PostMapping(Routes.MERCHANT_UPDATE_SHIPMENT)
    public String updateShipment(@RequestParam("uuid") String uuid,
                                 @RequestParam("nextState") String nextState,
                                 RedirectAttributes redirectAttributes) {
        if (uuid == null || uuid.isBlank()) {
            redirectAttributes.addAttribute("error", "Missing shipment UUID.");
            return "redirect:" + Routes.merchantShipmentsPage();
        }

        DeliveryState targetState;
        try {
            targetState = DeliveryState.valueOf(nextState.toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ex) {
            redirectAttributes.addAttribute("error", "Unknown delivery state.");
            redirectAttributes.addAttribute("uuid", uuid);
            return "redirect:" + Routes.merchantShipmentsPage();
        }

        ShippingLabelMetaDataEntity updated = shippingService.updateDeliveryState(uuid, targetState);
        if (updated == null) {
            redirectAttributes.addAttribute("error", "Unable to update shipment.");
            redirectAttributes.addAttribute("uuid", uuid);
            return "redirect:" + Routes.merchantShipmentsPage();
        }

        redirectAttributes.addAttribute("status", "Shipment updated to " + targetState + ".");
        redirectAttributes.addAttribute("uuid", updated.getUuid());
        return "redirect:" + Routes.merchantShipmentsPage();
    }

    @GetMapping(Routes.MERCHANT_ROUTES_PAGE)
    public String routesPage(Model model,
                             @RequestParam(value = "routeId", required = false) String routeId,
                             @RequestParam(value = "status", required = false) String status,
                             @RequestParam(value = "error", required = false) String error) {
        List<DeliveryRouteEntity> routes = routeService.getAllRoutes();
        DeliveryRouteEntity selected = null;
        if (routeId != null && !routeId.isBlank()) {
            selected = routeService.findRoute(routeId);
        }

        List<DeliveryRouteStopEntity> stops = selected != null
                ? routeService.getRouteStops(selected.getRouteId())
                : List.of();

        Map<String, ShippingLabelMetaDataEntity> labelLookup = new HashMap<>();
        for (DeliveryRouteStopEntity stop : stops) {
            if (stop == null || stop.getLabelUuid() == null) {
                continue;
            }
            ShippingLabelMetaDataEntity label = shippingService.findByUuid(stop.getLabelUuid());
            if (label != null) {
                labelLookup.put(stop.getLabelUuid(), label);
            }
        }

        model.addAttribute("title", "Routes");
        model.addAttribute("routes", routes);
        model.addAttribute("selectedRoute", selected);
        model.addAttribute("selectedStops", stops);
        model.addAttribute("labelLookup", labelLookup);
        model.addAttribute("routeStatus", status);
        model.addAttribute("routeError", error);
        return "dashboard/merchant/merchant-routes";
    }

    @PostMapping(Routes.MERCHANT_CREATE_ROUTE)
    public String createRoute(RedirectAttributes redirectAttributes) {
        DeliveryRouteEntity route = routeService.createRouteFromAllLabels();
        if (route == null) {
            redirectAttributes.addAttribute("error", "No shipments available to route.");
            return "redirect:" + Routes.merchantRoutesPage();
        }
        redirectAttributes.addAttribute("status", "Route created: " + route.getRouteId());
        redirectAttributes.addAttribute("routeId", route.getRouteId());
        return "redirect:" + Routes.merchantRoutesPage();
    }

    @PostMapping(Routes.MERCHANT_UPDATE_ROUTE)
    public String updateRoute(@RequestParam("routeId") String routeId,
                              @RequestParam(value = "status", required = false) String status,
                              @RequestParam(value = "notes", required = false) String notes,
                              RedirectAttributes redirectAttributes) {
        DeliveryRouteEntity updated = routeService.updateRoute(routeId, status, notes);
        if (updated == null) {
            redirectAttributes.addAttribute("error", "Unable to update route.");
            return "redirect:" + Routes.merchantRoutesPage();
        }
        redirectAttributes.addAttribute("status", "Route updated.");
        redirectAttributes.addAttribute("routeId", updated.getRouteId());
        return "redirect:" + Routes.merchantRoutesPage();
    }

    @PostMapping(Routes.MERCHANT_DELETE_ROUTE)
    public String deleteRoute(@RequestParam("routeId") String routeId,
                              RedirectAttributes redirectAttributes) {
        boolean deleted = routeService.deleteRoute(routeId);
        if (!deleted) {
            redirectAttributes.addAttribute("error", "Unable to delete route.");
            return "redirect:" + Routes.merchantRoutesPage();
        }
        redirectAttributes.addAttribute("status", "Route deleted.");
        return "redirect:" + Routes.merchantRoutesPage();
    }
}
