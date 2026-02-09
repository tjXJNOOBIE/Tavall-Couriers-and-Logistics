package org.tavall.couriers.web.view.css.page.templates;

public final class DashboardTemplateCss {

    private DashboardTemplateCss() {
        throw new IllegalStateException("Utility class");
    }

    public static final String OPS_GRID = "ops-grid";
    public static final String OPS_CARD = "ops-card";
    public static final String OPS_CARD_ALT = "ops-card ops-card-alt";
    public static final String SCAN_QUEUE_LIST = "scan-queue-list";
    public static final String SCAN_QUEUE_CARD = "scan-queue-card";
    public static final String SCAN_QUEUE_PROCESSING = "scan-queue-card scan-queue-processing";
    public static final String SCAN_QUEUE_ERROR = "scan-queue-card scan-queue-error";
    public static final String ROUTE_MODAL_PANEL = "route-modal-panel";
    public static final String ROUTE_ACTIONS = "route-actions";
    public static final String ROUTE_ACTION = "route-action";

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


