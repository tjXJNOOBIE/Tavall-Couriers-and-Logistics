package org.tavall.couriers.web.view.controller.home;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.tavall.couriers.api.web.endpoints.Routes;
import org.tavall.couriers.web.view.controller.home.helper.HomePageControllerHelper;

@Controller
public class HomePageController {
    private final HomePageControllerHelper helper;

    public HomePageController(HomePageControllerHelper helper) {
        this.helper = helper;
    }

    @GetMapping(Routes.HOME)
    public String home(Model model) {
        helper.populateHomeModel(model);
        return "home";
    }

}
