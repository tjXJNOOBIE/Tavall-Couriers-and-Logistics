package org.tavall.couriers.web.view.css.abstracted;

public final class AbstractedCss {

    private AbstractedCss() {
        throw new IllegalStateException("Utility class");
    }

    public static final String ENTRYPOINT = "";

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


