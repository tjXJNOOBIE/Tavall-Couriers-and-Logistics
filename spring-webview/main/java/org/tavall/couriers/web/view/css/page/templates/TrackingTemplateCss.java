package org.tavall.couriers.web.view.css.page.templates;

public final class TrackingTemplateCss {

    private TrackingTemplateCss() {
        throw new IllegalStateException("Utility class");
    }

    public static final String TRACKING_CARD = "tracking-card";
    public static final String TRACKING_ACTIONS = "tracking-actions";
    public static final String TRACKING_TOOLS = "tracking-tools";
    public static final String TRACKING_TOOL = "tracking-tool";

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


