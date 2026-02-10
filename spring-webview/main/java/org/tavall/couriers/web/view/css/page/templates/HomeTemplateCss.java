package org.tavall.couriers.web.view.css.page.templates;

public final class HomeTemplateCss {

    private HomeTemplateCss() {
        throw new IllegalStateException("Utility class");
    }

    public static final String BODY = "home-shell";
    public static final String MAIN_CONTENT = "main-content fade-in";
    public static final String CONTAINER = "container";
    public static final String HERO_CARD = "max-w-md mx-auto mt-10 bg-white p-8 rounded-2xl shadow-xl fade-in border border-slate-100";
    public static final String HERO_HEADER = "text-center mb-8";
    public static final String HERO_BRAND = "text-sm font-bold text-slate-400 uppercase mb-2";
    public static final String HERO_ICON_WRAP = "bg-blue-50 w-16 h-16 rounded-full flex items-center justify-center mx-auto mb-4";
    public static final String HERO_ICON = "text-blue-600 w-8 h-8";
    public static final String HERO_TITLE = "text-2xl font-bold text-slate-800";
    public static final String HERO_DESC = "text-slate-500 text-sm";
    public static final String ACTIONS = "home-actions";

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


