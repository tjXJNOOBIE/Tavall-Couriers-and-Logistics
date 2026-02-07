package org.tavall.couriers.api.web.endpoints.dashboard;

import org.tavall.couriers.api.web.endpoints.AppEndpoint;

public enum DefaultDashboardEndpoints implements AppEndpoint {

    // Dashboard homepage flow
    DASHBOARD_HOME("/dashboard", "GET"),
    DASHBOARD_HOME_ALIAS("/dashboard/home", "GET"),
    DASHBOARD_LOGIN_HOME("/dashboard/login", "GET"),

    // Session / auth flow (dashboard-scoped)
    DASHBOARD_LOGOUT("/dashboard/logout", "POST"),

    // Generic dashboard pages (not role dashboards)
    DASHBOARD_ACCESS_DENIED("/dashboard/denied", "GET"),
    DASHBOARD_ERROR("/dashboard/error", "GET"),

    // Optional: quick status page for dashboard UI sanity checks
    DASHBOARD_STATUS("/dashboard/status", "GET");

    public static final String DASHBOARD_HOME_PATH = "/dashboard";
    public static final String DASHBOARD_HOME_ALIAS_PATH = "/dashboard/home";
    public static final String DASHBOARD_LOGIN_HOME_PATH = "/dashboard/login";
    public static final String DASHBOARD_LOGOUT_PATH = "/dashboard/logout";
    public static final String DASHBOARD_ACCESS_DENIED_PATH = "/dashboard/denied";
    public static final String DASHBOARD_ERROR_PATH = "/dashboard/error";
    public static final String DASHBOARD_STATUS_PATH = "/dashboard/status";


    private final String endpoint;
    private final String method;

    DefaultDashboardEndpoints(String endpoint, String method) {
        this.endpoint = endpoint;
        this.method = method;
    }

    @Override
    public String endpoint() {
        return endpoint;
    }

    @Override
    public String method() {
        return method;
    }

}