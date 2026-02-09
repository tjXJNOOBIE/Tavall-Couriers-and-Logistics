package org.tavall.couriers.web.view.css.page;

public final class FlowCss {

    private FlowCss() {
        throw new IllegalStateException("Utility class");
    }

    public static final String FLEX_CENTER = "flex items-center justify-center";
    public static final String FLEX_BETWEEN = "flex items-center justify-between";
    public static final String TEXT_CENTER = "text-center";
    public static final String GRID_TWO = "grid grid-cols-2 gap-4";
    public static final String GRID_THREE = "grid grid-cols-3 gap-4";

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


