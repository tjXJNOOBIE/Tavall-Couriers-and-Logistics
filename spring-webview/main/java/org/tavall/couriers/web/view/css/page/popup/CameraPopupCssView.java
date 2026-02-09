package org.tavall.couriers.web.view.css.page.popup;

import org.springframework.stereotype.Component;

@Component("cameraPopupCss")
public class CameraPopupCssView {

    public String overlay() {
        return CameraPopupCss.SCAN_OVERLAY;
    }

    public String popup() {
        return CameraPopupCss.SCAN_POPUP;
    }

    public String popupCamera() {
        return CameraPopupCss.SCAN_POPUP_CAMERA;
    }

    public String topbar() {
        return CameraPopupCss.SCAN_TOPBAR;
    }

    public String popupFooter() {
        return CameraPopupCss.SCAN_POPUP_FOOTER;
    }

    public String close() {
        return CameraPopupCss.SCAN_CLOSE;
    }

    public String layout() {
        return CameraPopupCss.SCAN_LAYOUT;
    }

    public String layoutSingle() {
        return CameraPopupCss.SCAN_LAYOUT_SINGLE;
    }

    public String frame() {
        return CameraPopupCss.SCAN_FRAME;
    }

    public String videoWrap() {
        return CameraPopupCss.SCAN_VIDEO_WRAP;
    }

    public String stateOverlay() {
        return CameraPopupCss.SCAN_STATE_OVERLAY;
    }

    public String stateBadge() {
        return CameraPopupCss.SCAN_STATE_BADGE;
    }

    public String dataOverlay() {
        return CameraPopupCss.SCAN_DATA_OVERLAY;
    }

    public String dataFeed() {
        return CameraPopupCss.SCAN_DATA_FEED;
    }

    public String dataFlash() {
        return CameraPopupCss.SCAN_DATA_FEED;
    }

    public String panel() {
        return CameraPopupCss.SCAN_PANEL;
    }

    public String panelCard() {
        return CameraPopupCss.SCAN_PANEL_CARD;
    }

    public String panelHeader() {
        return CameraPopupCss.SCAN_PANEL_HEADER;
    }

    public String panelActions() {
        return CameraPopupCss.SCAN_PANEL_ACTIONS;
    }

    public String detailGrid() {
        return CameraPopupCss.SCAN_DETAIL_GRID;
    }

    public String field() {
        return CameraPopupCss.SCAN_FIELD;
    }

    public String fieldSpan() {
        return CameraPopupCss.SCAN_FIELD_SPAN;
    }

    public String actions() {
        return CameraPopupCss.SCAN_ACTIONS;
    }

    public String cameraActions() {
        return CameraPopupCss.SCAN_CAMERA_ACTIONS;
    }

    public String toggle() {
        return CameraPopupCss.SCAN_TOGGLE;
    }

    public String join(String... classes) {
        return CameraPopupCss.join(classes);
    }
}


