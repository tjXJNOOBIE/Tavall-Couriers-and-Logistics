package org.tavall.couriers.api.web.endpoints.dashboard.driver;


import org.tavall.couriers.api.web.endpoints.AppEndpoint;

public enum DriverDashboardEndpoints implements AppEndpoint {
    // "Have the user create a shipping label before we can scan QR"
    CREATE_LABEL("/internal/api/v1/driver/label/create", "POST"),

    // "Select the transition state... Continue to Scan Transition Completion"
    TRANSITION_PACKAGE("/internal/api/v1/driver/package/transition", "POST"),

    // "Pre-scan page... If no labels... Check availability"
    CHECK_LABEL_AVAILABILITY("/internal/api/v1/driver/label/check", "GET");

    public static final String DASHBOARD_PATH = "/dashboard/driver";
    public static final String CHECK_LABEL_AVAILABILITY_PATH = "/internal/api/v1/driver/label/check";
    public static final String SCAN_PAGE_PATH = "/dashboard/driver/scan";
    public static final String STATE_PAGE_PATH = "/dashboard/driver/state";
    public static final String CREATE_LABEL_PAGE_PATH = "/dashboard/driver/create-label";
    public static final String CREATE_LABEL_PATH = "/internal/api/v1/driver/label/create";
    public static final String TRANSITION_PACKAGE_PATH = "/internal/api/v1/driver/package/transition";

    private final String endpoint;
    private final String method;

    DriverDashboardEndpoints(String endpoint, String method) {
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
