package org.tavall.couriers.web.view.controller.dsahboard.merchant;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.tavall.couriers.api.web.endpoints.Routes;
import org.tavall.couriers.api.web.entities.ShippingLabelMetaDataEntity;
import org.tavall.couriers.web.view.controller.dsahboard.merchant.helper.MerchantDashboardControllerHelper;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Controller
@PreAuthorize("hasAnyRole('MERCHANT','SUPERUSER')")
public class MerchantDashboardController {

    private final MerchantDashboardControllerHelper helper;

    public MerchantDashboardController(MerchantDashboardControllerHelper helper) {
        this.helper = helper;
    }

    @GetMapping(Routes.MERCHANT_DASHBOARD)
    public String dashboard(Model model, @RequestParam(value = "created", required = false) String createdUuid) {
        return helper.dashboard(model, createdUuid);
    }

    @GetMapping(Routes.MERCHANT_CREATE_SHIPMENT_PAGE)
    public String createShipmentPage(Model model,
                                     @RequestParam(value = "created", required = false) String createdUuid) {
        return helper.createShipmentPage(model, createdUuid);
    }

    @PostMapping(Routes.MERCHANT_CREATE_SHIPMENT)
    public String createShipment(ShippingLabelMetaDataEntity shipment,
                                 @RequestParam(value = "deliverByDate", required = false)
                                 @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate deliverByDate,
                                 @RequestParam(value = "initialState", required = false) String initialState,
                                 @RequestParam(value = "source", required = false) String source,
                                 RedirectAttributes redirectAttributes) {
        return helper.createShipment(shipment, deliverByDate, initialState, source, redirectAttributes);
    }

    @GetMapping(Routes.MERCHANT_SCAN_PAGE)
    public String scanPage(Model model, @RequestParam(value = "uuid", required = false) String uuid) {
        return helper.scanPage(model, uuid);
    }

    @GetMapping(Routes.MERCHANT_SHIPMENTS_PAGE)
    public String shipmentsPage(Model model,
                                @RequestParam(value = "uuid", required = false) String uuid,
                                @RequestParam(value = "status", required = false) String status,
                                @RequestParam(value = "error", required = false) String error) {
        return helper.shipmentsPage(model, uuid, status, error);
    }

    @GetMapping(Routes.MERCHANT_SHIPMENTS_VIEW)
    public String shipmentsViewPage(Model model) {
        return helper.shipmentsViewPage(model);
    }

    @PostMapping(Routes.MERCHANT_UPDATE_SHIPMENT)
    public String updateShipment(@RequestParam("uuid") String uuid,
                                 @RequestParam("nextState") String nextState,
                                 RedirectAttributes redirectAttributes) {
        return helper.updateShipment(uuid, nextState, redirectAttributes);
    }

    @PostMapping(Routes.MERCHANT_DELETE_SHIPMENT)
    public String deleteShipment(@RequestParam("uuid") String uuid,
                                 RedirectAttributes redirectAttributes) {
        return helper.deleteShipment(uuid, redirectAttributes);
    }

    @GetMapping(Routes.MERCHANT_ROUTES_PAGE)
    public String routesPage(Model model,
                             @RequestParam(value = "routeId", required = false) String routeId,
                             @RequestParam(value = "status", required = false) String status,
                             @RequestParam(value = "error", required = false) String error) {
        return helper.routesPage(model, routeId, status, error);
    }

    @GetMapping(Routes.MERCHANT_ROUTE_DETAILS)
    public String routeDetails(@PathVariable("routeId") String routeId, Model model) {
        return helper.routeDetails(routeId, model);
    }

    @GetMapping("/dashboard/merchant/routes/{routeId}/scan")
    public String routeScan(@PathVariable("routeId") String routeId, Model model) {
        return helper.routeScan(routeId, model);
    }

    @PostMapping(Routes.MERCHANT_CREATE_ROUTE)
    public String createRoute(@RequestParam(value = "labelUuids", required = false) List<String> labelUuids,
                              @RequestParam(value = "assignedDriver", required = false) String assignedDriver,
                              @RequestParam(value = "deadline", required = false)
                              @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime deadline,
                              @RequestParam(value = "radiusMiles", required = false) String radiusMiles,
                              @RequestParam(value = "maxStops", required = false) String maxStops,
                              RedirectAttributes redirectAttributes) {
        return helper.createRoute(labelUuids, assignedDriver, deadline, radiusMiles, maxStops, redirectAttributes);
    }

    @PostMapping(Routes.MERCHANT_UPDATE_ROUTE)
    public String updateRoute(@RequestParam("routeId") String routeId,
                              @RequestParam(value = "status", required = false) String status,
                              @RequestParam(value = "notes", required = false) String notes,
                              RedirectAttributes redirectAttributes) {
        return helper.updateRoute(routeId, status, notes, redirectAttributes);
    }

    @PostMapping(Routes.MERCHANT_ADD_ROUTE_STOPS)
    public String addRouteStops(@RequestParam("routeId") String routeId,
                                @RequestParam(value = "labelUuids", required = false) List<String> labelUuids,
                                RedirectAttributes redirectAttributes) {
        return helper.addRouteStops(routeId, labelUuids, redirectAttributes);
    }

    @PostMapping(Routes.MERCHANT_ASSIGN_ROUTE_DRIVER)
    public String assignRouteDriver(@RequestParam("routeId") String routeId,
                                    @RequestParam(value = "assignedDriver", required = false) String assignedDriver,
                                    RedirectAttributes redirectAttributes) {
        return helper.assignRouteDriver(routeId, assignedDriver, redirectAttributes);
    }

    @PostMapping("/internal/api/v1/merchant/routes/scan/confirm")
    @ResponseBody
    public Map<String, String> confirmRouteScan(@RequestParam("routeId") String routeId,
                                                @RequestParam(value = "uuid", required = false) String uuid,
                                                @RequestParam(value = "trackingNumber", required = false) String trackingNumber,
                                                @RequestParam(value = "name", required = false) String name,
                                                @RequestParam(value = "address", required = false) String address,
                                                @RequestParam(value = "city", required = false) String city,
                                                @RequestParam(value = "state", required = false) String state,
                                                @RequestParam(value = "zip", required = false) String zip,
                                                @RequestParam(value = "country", required = false) String country,
                                                @RequestParam(value = "phone", required = false) String phone,
                                                @RequestParam(value = "deadline", required = false) String deadline) {
        return helper.confirmRouteScan(routeId, uuid, trackingNumber, name, address, city, state, zip, country, phone, deadline);
    }

    @PostMapping("/internal/api/v1/merchant/scan/intake/confirm")
    @ResponseBody
    public Map<String, Object> confirmIntake(@RequestParam(value = "uuid", required = false) String uuid,
                                             @RequestParam(value = "trackingNumber", required = false) String trackingNumber,
                                             @RequestParam(value = "name", required = false) String name,
                                             @RequestParam(value = "address", required = false) String address,
                                             @RequestParam(value = "city", required = false) String city,
                                             @RequestParam(value = "state", required = false) String state,
                                             @RequestParam(value = "zip", required = false) String zip,
                                             @RequestParam(value = "country", required = false) String country,
                                             @RequestParam(value = "phone", required = false) String phone,
                                             @RequestParam(value = "deadline", required = false) String deadline) {
        return helper.confirmIntake(uuid, trackingNumber, name, address, city, state, zip, country, phone, deadline);
    }

    @PostMapping(Routes.MERCHANT_DELETE_ROUTE)
    public String deleteRoute(@RequestParam("routeId") String routeId,
                              RedirectAttributes redirectAttributes) {
        return helper.deleteRoute(routeId, redirectAttributes);
    }

    @GetMapping("/internal/api/v1/merchant/routes/link")
    @ResponseBody
    public Map<String, Object> routeLinkStatus(@RequestParam("routeId") String routeId) {
        return helper.routeLinkStatus(routeId);
    }
}
