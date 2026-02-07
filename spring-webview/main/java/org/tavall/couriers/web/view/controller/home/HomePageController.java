package org.tavall.couriers.web.view.controller.home;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.tavall.couriers.api.shipping.database.entities.ShippingLabelMetaDataEntity;
import org.tavall.couriers.api.web.endpoints.page.PageViewEndpoints;
import org.tavall.couriers.api.web.repositories.ShippingLabelMetaDataRepository;
import org.tavall.couriers.web.view.button.ButtonConfig;

import java.time.Instant;
import java.util.Optional;

@Controller
public class HomePageController {
    private final ShippingLabelMetaDataRepository shippingRepo;

    public HomePageController(ShippingLabelMetaDataRepository shippingRepo) {
        this.shippingRepo = shippingRepo;
    }

    @GetMapping(PageViewEndpoints.HOME_PATH)
    public String home(Model model) {
        ButtonConfig regBtn = new ButtonConfig();
        regBtn.setLabel("Register");
        regBtn.setCooldownMs(4000);
        regBtn.setEnabled(true);
        regBtn.setToastMessage("Registration is coming soon!");

        model.addAttribute("regConfig", regBtn);
        return "home"; // maps to templates/home.html
    }

    private ShippingLabelMetaDataEntity loadLabel(String uuid) {
        if (uuid != null && !uuid.isBlank()) {
            Optional<ShippingLabelMetaDataEntity> byId = shippingRepo.findById(uuid);
            return byId.orElse(null);
        }
        return shippingRepo.findAll().stream().findFirst().orElse(null);
    }
}