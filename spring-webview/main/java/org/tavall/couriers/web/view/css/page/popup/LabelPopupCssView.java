package org.tavall.couriers.web.view.css.page.popup;

import org.springframework.stereotype.Component;

@Component("labelPopupCss")
public class LabelPopupCssView {

    public String panel() {
        return LabelPopupCss.LABEL_POPUP_PANEL;
    }

    public String frame() {
        return LabelPopupCss.LABEL_POPUP_FRAME;
    }

    public String closeButton() {
        return LabelPopupCss.LABEL_POPUP_CLOSE;
    }

    public String topbar() {
        return LabelPopupCss.LABEL_POPUP_TOPBAR;
    }

    public String detailsGrid() {
        return LabelPopupCss.LABEL_POPUP_DETAILS_GRID;
    }

    public String join(String... classes) {
        return LabelPopupCss.join(classes);
    }
}


