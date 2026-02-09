package org.tavall.couriers.web.view.css.page.components;

import org.springframework.stereotype.Component;

@Component("buttonsCss")
public class ButtonsCssView {

    public String btn() {
        return ButtonsCss.BTN;
    }

    public String btnPrimary() {
        return ButtonsCss.BTN_PRIMARY;
    }

    public String btnSecondary() {
        return ButtonsCss.BTN_SECONDARY;
    }

    public String btnOutline() {
        return ButtonsCss.BTN_OUTLINE;
    }
}
