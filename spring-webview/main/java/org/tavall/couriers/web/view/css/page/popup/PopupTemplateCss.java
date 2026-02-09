package org.tavall.couriers.web.view.css.page.popup;

public final class PopupTemplateCss {

    private PopupTemplateCss() {
        throw new IllegalStateException("Utility class");
    }

    public static final String SCAN_MODAL = "scan-modal";
    public static final String SCAN_MODAL_BACKDROP = "scan-modal-backdrop";
    public static final String SCAN_MODAL_PANEL = "scan-modal-panel";
    public static final String TAVALL_POPUP_PANEL = "tavall-popup-panel";
    public static final String SCAN_MODAL_FRAME = "scan-modal-frame";
    public static final String SCAN_MODAL_CLOSE = "scan-modal-close";
    public static final String SCAN_MODAL_CLOSE_OUTSIDE = "scan-modal-close-outside";

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


