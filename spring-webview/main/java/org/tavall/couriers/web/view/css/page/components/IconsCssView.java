package org.tavall.couriers.web.view.css.page.components;

import org.springframework.stereotype.Component;

@Component("iconsCss")
public class IconsCssView {

    public String icon() {
        return IconsCss.ICON;
    }

    public String iconLg() {
        return IconsCss.ICON_LG;
    }

    public String iconXl() {
        return IconsCss.ICON_XL;
    }
}
