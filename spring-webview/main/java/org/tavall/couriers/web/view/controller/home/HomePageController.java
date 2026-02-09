package org.tavall.couriers.web.view.controller.home;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.tavall.couriers.api.web.endpoints.Routes;

@Controller
public class HomePageController {
    public HomePageController() {
    }

    @GetMapping(Routes.HOME)
    public String home(Model model) {
        model.addAttribute("title", "Home");
        return "home"; // maps to templates/home.html
    }

}
