package org.tavall.couriers.web.view.css.page.popup;

public final class LabelPopupCss {

    private LabelPopupCss() {
        throw new IllegalStateException("Utility class");
    }

    public static final String LABEL_POPUP_PANEL = "label-popup-panel";
    public static final String LABEL_POPUP_FRAME = "label-popup-frame";
    public static final String LABEL_POPUP_CLOSE = "label-popup-close";
    public static final String LABEL_POPUP_TOPBAR = "label-popup-topbar";
    public static final String LABEL_POPUP_DETAILS_GRID = "label-popup-details-grid";

    public static String join(String... classes) {
        if (classes == null || classes.length == 0) {
            return "";
        }
        StringBuilder sb = new StringBuilder();
        for (String cls : classes) {
            if (cls == null) {
                continue;
            }
            String trimmed = cls.trim();
            if (trimmed.isEmpty()) {
                continue;
            }
            if (!sb.isEmpty()) {
                sb.append(' ');
            }
            sb.append(trimmed);
        }
        return sb.toString();
    }
}


