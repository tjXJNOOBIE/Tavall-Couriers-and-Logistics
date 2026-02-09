package org.tavall.couriers.web.view.css.page;

public final class BackgroundCss {

    private BackgroundCss() {
        throw new IllegalStateException("Utility class");
    }

    public static final String MAIN_CONTENT = "main-content";
    public static final String CONTAINER = "container";
    public static final String LOGIN_WRAPPER = "login-wrapper";
    public static final String CARD_BLUE = "card-blue";
    public static final String CARD_DARK = "card-dark";

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


