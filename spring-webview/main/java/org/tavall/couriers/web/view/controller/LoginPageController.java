package org.tavall.couriers.web.view.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.tavall.couriers.api.web.endpoints.user.UserAuthEndpoints;
import org.tavall.couriers.api.web.endpoints.page.PageViewEndpoints;

@Controller
public class LoginPageController {

    @GetMapping(PageViewEndpoints.LOGIN_PATH)
    public String login() {
        // Maps to src/main/resources/templates/login.html
        return "login";
    }

    @PostMapping(UserAuthEndpoints.LOGIN_PATH)
    public String loginInternal() {
        // Maps to src/main/resources/templates/login.html
        return "login";
    }
}
