package org.tavall.couriers.web.view.controller.dsahboard.driver;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.tavall.couriers.api.web.endpoints.Routes;
import org.tavall.couriers.api.web.entities.ShippingLabelMetaDataEntity;
import org.tavall.couriers.web.view.controller.dsahboard.driver.helper.DriverDashboardControllerHelper;

import java.time.LocalDate;

@Controller
@PreAuthorize("hasAnyRole('DRIVER','SUPERUSER')")
public class DriverDashboardController {

    private final DriverDashboardControllerHelper helper;

    public DriverDashboardController(DriverDashboardControllerHelper helper) {
        this.helper = helper;
    }

    @GetMapping(Routes.DRIVER_DASHBOARD)
    public String dashboard(Model model,
                            Authentication authentication,
                            @RequestParam(value = "routeId", required = false) String routeId) {
        return helper.dashboard(model, authentication, routeId);
    }

    @GetMapping(Routes.DRIVER_CREATE_LABEL_PAGE)
    public String createLabelPage(Model model,
                                  @RequestParam(value = "created", required = false) String createdUuid) {
        return helper.createLabelPage(model, createdUuid);
    }

    @PostMapping(Routes.DRIVER_CREATE_LABEL)
    public String createLabel(ShippingLabelMetaDataEntity shipment,
                              @RequestParam(value = "deliverByDate", required = false)
                              @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate deliverByDate,
                              Model model,
                              RedirectAttributes redirectAttributes) {
        return helper.createLabel(shipment, deliverByDate, model, redirectAttributes);
    }

    @GetMapping(Routes.DRIVER_SCAN_PAGE)
    public String scanPage(Model model) {
        return helper.scanPage(model);
    }

    @GetMapping(Routes.DRIVER_STATE_PAGE)
    public String statePage(Model model,
                            @RequestParam(value = "uuid", required = false) String uuid,
                            @RequestParam(value = "status", required = false) String status,
                            @RequestParam(value = "error", required = false) String error) {
        return helper.statePage(model, uuid, status, error);
    }

    @PostMapping(Routes.DRIVER_TRANSITION_PACKAGE)
    public String transitionPackage(@RequestParam("uuid") String uuid,
                                    @RequestParam("nextState") String nextState,
                                    RedirectAttributes redirectAttributes) {
        return helper.transitionPackage(uuid, nextState, redirectAttributes);
    }
}
