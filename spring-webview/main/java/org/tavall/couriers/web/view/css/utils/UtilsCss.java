package org.tavall.couriers.web.view.css.utils;

public final class UtilsCss {

    private UtilsCss() {
        throw new IllegalStateException("Utility class");
    }

    public static final String GRID = "grid";
    public static final String GRID_2 = "grid-2";
    public static final String GRID_3 = "grid-3";
    public static final String TEXT_CENTER = "text-center";
    public static final String TEXT_LINK = "text-link";
    public static final String MT_2 = "mt-2";
    public static final String MB_2 = "mb-2";
    public static final String HIDDEN_MOBILE = "hidden-mobile";
    public static final String FADE_IN = "fade-in";
    public static final String IS_HIDDEN = "is-hidden";
    public static final String TEXT_NOWRAP = "text-nowrap";

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


