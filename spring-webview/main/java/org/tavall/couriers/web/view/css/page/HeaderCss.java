package org.tavall.couriers.web.view.css.page;

public final class HeaderCss {

    private HeaderCss() {
        throw new IllegalStateException("Utility class");
    }

    public static final String NAVBAR = "navbar";
    public static final String BRAND = "brand";
    public static final String BRAND_TEXT = "brand-text";
    public static final String BRAND_LOGO = "brand-logo";
    public static final String BRAND_LOGO_ICON = "brand-logo-icon";
    public static final String USER_CONTROLS = "user-controls";
    public static final String USER_INFO = "user-info";
    public static final String USER_NAME = "user-name";
    public static final String USER_ROLE = "user-role";
    public static final String BTN_ICON = "btn-icon";
    public static final String USER_TRAY_BACKDROP = "user-tray-backdrop";
    public static final String USER_TRAY = "user-tray";
    public static final String USER_TRAY_PANEL = "user-tray-panel";
    public static final String USER_TRAY_LINKS = "user-tray-links";
    public static final String USER_TRAY_SECTION_TITLE = "user-tray-section-title";
    public static final String USER_TRAY_LINK = "user-tray-link";
    public static final String USER_TRAY_ACTIONS = "user-tray-actions";
    public static final String USER_TRAY_CLOSE = "user-tray-close";

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


