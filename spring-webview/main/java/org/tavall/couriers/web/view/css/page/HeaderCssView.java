package org.tavall.couriers.web.view.css.page;

import org.springframework.stereotype.Component;

@Component("headerCss")
public class HeaderCssView {

    public String navbar() {
        return HeaderCss.NAVBAR;
    }

    public String brand() {
        return HeaderCss.BRAND;
    }

    public String brandText() {
        return HeaderCss.BRAND_TEXT;
    }

    public String brandLogo() {
        return HeaderCss.BRAND_LOGO;
    }

    public String brandLogoIcon() {
        return HeaderCss.BRAND_LOGO_ICON;
    }

    public String userControls() {
        return HeaderCss.USER_CONTROLS;
    }

    public String userInfo() {
        return HeaderCss.USER_INFO;
    }

    public String userName() {
        return HeaderCss.USER_NAME;
    }

    public String userRole() {
        return HeaderCss.USER_ROLE;
    }

    public String btnIcon() {
        return HeaderCss.BTN_ICON;
    }

    public String userTrayBackdrop() {
        return HeaderCss.USER_TRAY_BACKDROP;
    }

    public String userTray() {
        return HeaderCss.USER_TRAY;
    }

    public String userTrayPanel() {
        return HeaderCss.USER_TRAY_PANEL;
    }

    public String userTrayLinks() {
        return HeaderCss.USER_TRAY_LINKS;
    }

    public String userTraySectionTitle() {
        return HeaderCss.USER_TRAY_SECTION_TITLE;
    }

    public String userTrayLink() {
        return HeaderCss.USER_TRAY_LINK;
    }

    public String userTrayActions() {
        return HeaderCss.USER_TRAY_ACTIONS;
    }

    public String userTrayClose() {
        return HeaderCss.USER_TRAY_CLOSE;
    }

    public String join(String... classes) {
        return HeaderCss.join(classes);
    }
}


