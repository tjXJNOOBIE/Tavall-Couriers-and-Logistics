package org.tavall.couriers.web.view.controller.home;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.tavall.couriers.api.web.endpoints.Routes;
import org.tavall.couriers.web.view.button.ButtonConfig;

@Controller
public class HomePageController {
    public HomePageController() {
    }

    @GetMapping(Routes.HOME)
    public String home(Model model) {
        ButtonConfig regBtn = new ButtonConfig();
        regBtn.setLabel("Register");
        regBtn.setCooldownMs(4000);
        regBtn.setEnabled(true);
        regBtn.setToastMessage("Registration is coming soon!");

        model.addAttribute("regConfig", regBtn);
        model.addAttribute("title", "Home");
        return "home"; // maps to templates/home.html
    }

}
