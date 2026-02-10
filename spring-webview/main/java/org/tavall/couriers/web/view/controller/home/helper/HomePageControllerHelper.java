package org.tavall.couriers.web.view.controller.home.helper;

import org.springframework.stereotype.Component;
import org.springframework.ui.Model;

@Component
public class HomePageControllerHelper {

    public void populateHomeModel(Model model) {
        model.addAttribute("title", "Home");
    }
}
