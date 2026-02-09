package org.tavall.couriers.web.view.css.camera;

import org.springframework.stereotype.Component;

@Component("cameraCss")
public class CameraCssView {

    public String cameraBase() {
        return CameraCss.CAMERA_BASE;
    }

    public String join(String... classes) {
        return CameraCss.join(classes);
    }
}


