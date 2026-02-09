package org.tavall.couriers.web.view.css.page.popup;

import org.springframework.stereotype.Component;

@Component("popupTemplateCss")
public class PopupTemplateCssView {

    public String scanModal() {
        return PopupTemplateCss.SCAN_MODAL;
    }

    public String scanModalBackdrop() {
        return PopupTemplateCss.SCAN_MODAL_BACKDROP;
    }

    public String scanModalPanel() {
        return PopupTemplateCss.SCAN_MODAL_PANEL;
    }

    public String tavallPopupPanel() {
        return PopupTemplateCss.TAVALL_POPUP_PANEL;
    }

    public String scanModalFrame() {
        return PopupTemplateCss.SCAN_MODAL_FRAME;
    }

    public String scanModalClose() {
        return PopupTemplateCss.SCAN_MODAL_CLOSE;
    }

    public String scanModalCloseOutside() {
        return PopupTemplateCss.SCAN_MODAL_CLOSE_OUTSIDE;
    }

    public String join(String... classes) {
        return PopupTemplateCss.join(classes);
    }
}


