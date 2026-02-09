package org.tavall.couriers.web.view.controller.dsahboard.merchant;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.tavall.couriers.api.delivery.state.DeliveryState;
import org.tavall.couriers.api.qr.scan.cache.ScanCacheService;
import org.tavall.couriers.api.qr.scan.cache.ScanErrorCacheService;
import org.tavall.couriers.api.qr.scan.metadata.ScanResponse;
import org.tavall.couriers.api.web.entities.DeliveryRouteEntity;
import org.tavall.couriers.api.web.entities.DeliveryRouteStopEntity;
import org.tavall.couriers.api.web.entities.ShippingLabelMetaDataEntity;
import org.tavall.couriers.api.web.endpoints.Routes;
import org.tavall.couriers.api.web.service.camera.CameraPageService;
import org.tavall.couriers.api.web.service.route.DeliveryRouteService;
import org.tavall.couriers.api.web.service.route.GoogleMapsRouteBuilder;
import org.tavall.couriers.api.web.service.route.RouteConstants;
import org.tavall.couriers.api.web.service.shipping.ShippingLabelMetaDataService;
import org.tavall.couriers.api.web.service.user.UserAccountService;
import org.tavall.couriers.api.web.user.UserAccountEntity;
import org.tavall.couriers.api.web.user.permission.Role;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Locale;
import java.util.UUID;
import java.util.stream.Collectors;

@Controller
public class MerchantDashboardController {

    private final ShippingLabelMetaDataService shippingService;
    private final DeliveryRouteService routeService;
    private final ScanCacheService scanCache;
    private final ScanErrorCacheService scanErrorCache;
    private final GoogleMapsRouteBuilder routeLinkBuilder = new GoogleMapsRouteBuilder();
    private final CameraPageService cameraPageService;
    private final UserAccountService userAccountService;

    public MerchantDashboardController(ShippingLabelMetaDataService shippingService,
                                       DeliveryRouteService routeService,
                                       ScanCacheService scanCache,
                                       ScanErrorCacheService scanErrorCache,
                                       CameraPageService cameraPageService,
                                       UserAccountService userAccountService) {
        this.shippingService = shippingService;
        this.routeService = routeService;
        this.scanCache = scanCache;
        this.scanErrorCache = scanErrorCache;
        this.cameraPageService = cameraPageService;
        this.userAccountService = userAccountService;
    }

    @GetMapping(Routes.MERCHANT_DASHBOARD)
    public String dashboard(Model model, @RequestParam(value = "created", required = false) String createdUuid) {
        List<ShippingLabelMetaDataEntity> labels = shippingService.getAllShipmentLabels();
        List<DeliveryRouteEntity> routes = routeService.getAllRoutes();
        int routeCount = routes.size();
        List<ScanResponse> scanQueue = scanCache.getRecentResponses(4);
        List<ScanResponse> scanProcessing = cameraPageService.getProcessingResponses();
        List<ScanResponse> scanErrors = scanErrorCache.getRecentErrors(2);
        List<DeliveryRouteEntity> routeSnapshot = routes.stream()
                .sorted((a, b) -> {
                    Instant aDeadline = a != null ? a.getDeadline() : null;
                    Instant bDeadline = b != null ? b.getDeadline() : null;
                    if (aDeadline == null && bDeadline == null) return 0;
                    if (aDeadline == null) return 1;
                    if (bDeadline == null) return -1;
                    return aDeadline.compareTo(bDeadline);
                })
                .limit(3)
                .toList();
        model.addAttribute("title", "Merchant Hub");
        model.addAttribute("shipment", new ShippingLabelMetaDataEntity());
        model.addAttribute("allStates", Arrays.asList(DeliveryState.values()));
        model.addAttribute("scanQueue", scanQueue);
        model.addAttribute("scanProcessing", scanProcessing);
        model.addAttribute("scanErrors", scanErrors);
        model.addAttribute("totalShipments", labels.size());
        model.addAttribute("routeCount", routeCount);
        model.addAttribute("routeSnapshot", routeSnapshot);
        model.addAttribute("scanErrorCount", scanErrorCache.getErrorCount());
        model.addAttribute("firstScanErrorUuid", scanErrorCache.getLatestErrorUuid());

        if (createdUuid != null && !createdUuid.isBlank()) {
            ShippingLabelMetaDataEntity created = shippingService.findByUuid(createdUuid);
            if (created != null) {
                model.addAttribute("createdLabel", created);
            }
        }

        return "dashboard/merchant/merchant-dashboard";
    }

    @GetMapping(Routes.MERCHANT_CREATE_SHIPMENT_PAGE)
    public String createShipmentPage(Model model,
                                     @RequestParam(value = "created", required = false) String createdUuid) {
        model.addAttribute("title", "Manual Label");
        model.addAttribute("shipment", new ShippingLabelMetaDataEntity());
        model.addAttribute("allStates", Arrays.asList(DeliveryState.values()));

        if (createdUuid != null && !createdUuid.isBlank()) {
            ShippingLabelMetaDataEntity created = shippingService.findByUuid(createdUuid);
            if (created != null) {
                model.addAttribute("createdLabel", created);
            }
        }

        return "dashboard/merchant/merchant-create-shipment";
    }

    @PostMapping(Routes.MERCHANT_CREATE_SHIPMENT)
    public String createShipment(ShippingLabelMetaDataEntity shipment,
                                 @RequestParam(value = "deliverByDate", required = false)
                                 @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate deliverByDate,
                                 @RequestParam(value = "initialState", required = false) String initialState,
                                 @RequestParam(value = "source", required = false) String source,
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
        if (source != null && source.equalsIgnoreCase("popup")) {
            redirectAttributes.addAttribute("created", created.getUuid());
            return "redirect:" + Routes.merchantCreateShipmentPage();
        }
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
        List<ShippingLabelMetaDataEntity> labels = shippingService.getAllShipmentLabels();
        List<UserAccountEntity> drivers = loadDriverAccounts();
        Map<UUID, String> driverLookup = toDriverLookup(drivers);
        DeliveryRouteEntity selected = null;
        if (routeId != null && !routeId.isBlank()) {
            selected = routeService.findRoute(routeId);
        }

        List<DeliveryRouteStopEntity> stops = selected != null
                ? routeService.getRouteStops(selected.getRouteId())
                : List.of();

        Map<String, ShippingLabelMetaDataEntity> labelLookup = new HashMap<>();
        Map<String, DeliveryRouteStopEntity> stopLookup = new HashMap<>();
        for (DeliveryRouteStopEntity stop : stops) {
            if (stop == null || stop.getLabelUuid() == null) {
                continue;
            }
            stopLookup.put(stop.getLabelUuid(), stop);
            ShippingLabelMetaDataEntity label = shippingService.findByUuid(stop.getLabelUuid());
            if (label != null) {
                labelLookup.put(stop.getLabelUuid(), label);
            }
        }

        List<ShippingLabelMetaDataEntity> availableStops = labels.stream()
                .filter(label -> label != null && label.getUuid() != null && !stopLookup.containsKey(label.getUuid()))
                .toList();

        model.addAttribute("title", "Routes");
        model.addAttribute("routes", routes);
        model.addAttribute("selectedRoute", selected);
        model.addAttribute("selectedStops", stops);
        model.addAttribute("labelLookup", labelLookup);
        model.addAttribute("availableLabels", labels);
        model.addAttribute("availableStops", availableStops);
        model.addAttribute("drivers", drivers);
        model.addAttribute("driverLookup", driverLookup);
        model.addAttribute("routeStatus", status);
        model.addAttribute("routeError", error);
        model.addAttribute("routeRadiusDefault", routeService.getDefaultRadiusMiles());
        model.addAttribute("routeMaxDefault", routeService.getDefaultMaxStops());
        return "dashboard/merchant/merchant-routes";
    }

    @GetMapping(Routes.MERCHANT_ROUTE_DETAILS)
    public String routeDetails(@PathVariable("routeId") String routeId, Model model) {
        DeliveryRouteEntity route = routeService.findRoute(routeId);
        List<DeliveryRouteStopEntity> stops = route != null ? routeService.getRouteStops(routeId) : List.of();
        if (!stops.isEmpty()) {
            stops = stops.stream()
                    .sorted((a, b) -> Integer.compare(
                            a != null ? a.getStopOrder() : Integer.MAX_VALUE,
                            b != null ? b.getStopOrder() : Integer.MAX_VALUE))
                    .toList();
        }
        Map<String, ShippingLabelMetaDataEntity> labelLookup = new HashMap<>();
        for (DeliveryRouteStopEntity stop : stops) {
            if (stop == null) {
                continue;
            }
            String uuid = stop.getLabelUuid();
            if (uuid == null || uuid.isBlank() || labelLookup.containsKey(uuid)) {
                continue;
            }
            ShippingLabelMetaDataEntity label = shippingService.findByUuid(uuid);
            if (label != null) {
                labelLookup.put(uuid, label);
            }
        }
        List<UserAccountEntity> drivers = loadDriverAccounts();
        Map<UUID, String> driverLookup = toDriverLookup(drivers);
        String driverName = "Unassigned";
        if (route != null && route.getAssignedDrivers() != null) {
            driverName = driverLookup.getOrDefault(route.getAssignedDrivers(), route.getAssignedDrivers().toString());
        }
        model.addAttribute("title", "Route Details");
        model.addAttribute("route", route);
        model.addAttribute("stops", stops);
        model.addAttribute("labelLookup", labelLookup);
        model.addAttribute("driverName", driverName);
        model.addAttribute("routeStatus", route != null ? route.getStatus() : "Unavailable");
        String routeLink = route != null && route.getRouteLink() != null && !route.getRouteLink().isBlank()
                ? route.getRouteLink()
                : resolveRouteLink(stops, labelLookup);
        model.addAttribute("routeLink", routeLink);
        return "dashboard/merchant/merchant-route-details";
    }

    @GetMapping("/dashboard/merchant/routes/{routeId}/scan")
    public String routeScan(@PathVariable("routeId") String routeId, Model model) {
        model.addAttribute("routeId", routeId);
        model.addAttribute("title", "Route Scan");
        return "dashboard/merchant/merchant-route-scan";
    }

    @PostMapping(Routes.MERCHANT_CREATE_ROUTE)
    public String createRoute(@RequestParam(value = "labelUuids", required = false) List<String> labelUuids,
                              @RequestParam(value = "assignedDriver", required = false) String assignedDriver,
                              @RequestParam(value = "deadline", required = false)
                              @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime deadline,
                              @RequestParam(value = "radiusMiles", required = false) String radiusMiles,
                              @RequestParam(value = "maxStops", required = false) String maxStops,
                              RedirectAttributes redirectAttributes) {
        UUID assignedDriverId = parseUuid(assignedDriver);
        Instant routeDeadline = deadline != null
                ? deadline.atZone(ZoneId.systemDefault()).toInstant()
                : null;
        Double radius = parseDouble(radiusMiles);
        Integer max = parseInt(maxStops);
        DeliveryRouteEntity route = routeService.createRouteFromLabels(labelUuids, assignedDriverId, routeDeadline, radius, max);
        if (route == null) {
            redirectAttributes.addAttribute("error", "Select at least one shipment to create a route.");
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

    @PostMapping(Routes.MERCHANT_ADD_ROUTE_STOPS)
    public String addRouteStops(@RequestParam("routeId") String routeId,
                                @RequestParam(value = "labelUuids", required = false) List<String> labelUuids,
                                RedirectAttributes redirectAttributes) {
        DeliveryRouteEntity updated = routeService.addStops(routeId, labelUuids);
        if (updated == null) {
            redirectAttributes.addAttribute("error", "Select at least one new shipment to add.");
            redirectAttributes.addAttribute("routeId", routeId);
            return "redirect:" + Routes.merchantRoutesPage();
        }
        redirectAttributes.addAttribute("status", "Stops added to route.");
        redirectAttributes.addAttribute("routeId", updated.getRouteId());
        return "redirect:" + Routes.merchantRoutesPage();
    }

    @PostMapping(Routes.MERCHANT_ASSIGN_ROUTE_DRIVER)
    public String assignRouteDriver(@RequestParam("routeId") String routeId,
                                    @RequestParam(value = "assignedDriver", required = false) String assignedDriver,
                                    RedirectAttributes redirectAttributes) {
        UUID driverId = parseUuid(assignedDriver);
        DeliveryRouteEntity updated = routeService.assignDriver(routeId, driverId);
        if (updated == null) {
            redirectAttributes.addAttribute("error", "Unable to assign driver.");
            return "redirect:" + Routes.merchantRoutesPage();
        }
        redirectAttributes.addAttribute("status", "Route assignment updated.");
        redirectAttributes.addAttribute("routeId", updated.getRouteId());
        return "redirect:" + Routes.merchantRoutesPage();
    }

    @PostMapping("/internal/api/v1/merchant/routes/scan/confirm")
    @ResponseBody
    public Map<String, String> confirmRouteScan(@RequestParam("routeId") String routeId,
                                                @RequestParam(value = "uuid", required = false) String uuid,
                                                @RequestParam(value = "name", required = false) String name,
                                                @RequestParam(value = "address", required = false) String address,
                                                @RequestParam(value = "city", required = false) String city,
                                                @RequestParam(value = "state", required = false) String state,
                                                @RequestParam(value = "zip", required = false) String zip,
                                                @RequestParam(value = "country", required = false) String country,
                                                @RequestParam(value = "phone", required = false) String phone,
                                                @RequestParam(value = "deadline", required = false) String deadline) {
        ShippingLabelMetaDataEntity request = new ShippingLabelMetaDataEntity();
        request.setRecipientName(name);
        request.setAddress(address);
        request.setCity(city);
        request.setState(state);
        request.setZipCode(zip);
        request.setCountry(country);
        request.setPhoneNumber(phone);
        if (deadline != null && !deadline.isBlank()) {
            try {
                request.setDeliverBy(Instant.parse(deadline));
            } catch (Exception ignored) {
                request.setDeliverBy(null);
            }
        }

        ShippingLabelMetaDataEntity created = uuid != null && !uuid.isBlank()
                ? shippingService.createShipmentWithUuid(request, uuid, DeliveryState.LABEL_CREATED)
                : shippingService.createShipment(request, DeliveryState.LABEL_CREATED);

        routeService.addStopAsync(routeId, created.getUuid());
        return Map.of(
                "status", "added",
                "uuid", created.getUuid()
        );
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

    @GetMapping("/internal/api/v1/merchant/routes/link")
    @ResponseBody
    public Map<String, Object> routeLinkStatus(@RequestParam("routeId") String routeId) {
        DeliveryRouteEntity route = routeService.findRoute(routeId);
        String link = route != null ? route.getRouteLink() : null;
        boolean ready = link != null && !link.isBlank();
        return Map.of(
                "ready", ready,
                "routeLink", ready ? link : ""
        );
    }

    private String resolveRouteLink(List<DeliveryRouteStopEntity> stops,
                                    Map<String, ShippingLabelMetaDataEntity> labelLookup) {
        if (stops == null || stops.isEmpty() || labelLookup == null || labelLookup.isEmpty()) {
            return null;
        }
        List<String> addresses = new ArrayList<>();
        if (RouteConstants.HQ_START_ADDRESS != null && !RouteConstants.HQ_START_ADDRESS.isBlank()) {
            addresses.add(RouteConstants.HQ_START_ADDRESS);
        }
        for (DeliveryRouteStopEntity stop : stops) {
            if (stop == null) {
                continue;
            }
            ShippingLabelMetaDataEntity label = labelLookup.get(stop.getLabelUuid());
            if (label == null) {
                continue;
            }
            String formatted = formatStopAddress(label);
            if (!formatted.isBlank()) {
                addresses.add(formatted);
            }
        }
        if (addresses.isEmpty()) {
            return null;
        }
        GoogleMapsRouteBuilder.RouteLinkResult result = routeLinkBuilder.buildRouteLink(addresses);
        if (result == null) {
            return null;
        }
        String routeUrl = result.routeUrl();
        return routeUrl != null && !routeUrl.isBlank() ? routeUrl : null;
    }

    private String formatStopAddress(ShippingLabelMetaDataEntity label) {
        if (label == null) {
            return "";
        }
        StringBuilder builder = new StringBuilder();
        appendAddressPart(builder, label.getAddress());
        appendAddressPart(builder, label.getCity());
        appendAddressPart(builder, label.getState());
        appendZipCode(builder, label.getZipCode());
        return builder.toString().trim();
    }

    private void appendAddressPart(StringBuilder builder, String part) {
        if (part == null || part.isBlank()) {
            return;
        }
        if (builder.length() > 0) {
            builder.append(", ");
        }
        builder.append(part.trim());
    }

    private void appendZipCode(StringBuilder builder, String zip) {
        if (zip == null || zip.isBlank()) {
            return;
        }
        if (builder.length() > 0) {
            builder.append(" ");
        }
        builder.append(zip.trim());
    }

    private List<UserAccountEntity> loadDriverAccounts() {
        return userAccountService.getAllUsers().stream()
                .filter(user -> user != null
                        && user.getRoles() != null
                        && user.getRoles().contains(Role.DRIVER))
                .collect(Collectors.toList());
    }

    private Map<UUID, String> toDriverLookup(List<UserAccountEntity> drivers) {
        if (drivers == null || drivers.isEmpty()) {
            return Map.of();
        }
        return drivers.stream()
                .filter(driver -> driver.getUserUUID() != null)
                .collect(Collectors.toMap(UserAccountEntity::getUserUUID, UserAccountEntity::getUsername, (a, b) -> a));
    }

    private UUID parseUuid(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return UUID.fromString(value.trim());
        } catch (IllegalArgumentException ex) {
            return null;
        }
    }

    private Double parseDouble(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return Double.parseDouble(value);
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    private Integer parseInt(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException ex) {
            return null;
        }
    }
}
