package org.tavall.couriers.web.view.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.tavall.couriers.web.view.css.page.popup.CameraPopupCss;

@RestController
public class StyleController {

    @GetMapping(value = "/internal/api/v1/styles/camera-popup.css", produces = "text/css")
    public String cameraPopupCss() {
        return CameraPopupCss.CAMERA_POPUP_CSS;
    }
}

