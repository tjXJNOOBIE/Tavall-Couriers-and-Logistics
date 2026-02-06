package org.tavall.couriers.api.web.endpoints.dashboard.driver;


import org.tavall.couriers.api.web.endpoints.AppEndpoint;

public enum DriverEndpoints implements AppEndpoint {
    // "Have the user create a shipping label before we can scan QR"
    CREATE_LABEL("/internal/api/v1/driver/label/create", "POST"),

    // "Select the transition state... Continue to Scan Transition Completion"
    TRANSITION_PACKAGE("/internal/api/v1/driver/package/transition", "POST"),

    // "Pre-scan page... If no labels... Check availability"
    CHECK_LABEL_AVAILABILITY("/internal/api/v1/driver/label/check", "GET");

    private final String endpoint;
    private final String method;

    DriverEndpoints(String endpoint, String method) {
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