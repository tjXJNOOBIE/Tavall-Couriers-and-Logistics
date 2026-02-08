package org.tavall.couriers.api.web.endpoints.dashboard.merchant;


import org.tavall.couriers.api.web.endpoints.AppEndpoint;

public enum MerchantDashboardEndpoints implements AppEndpoint {

    // Merchants have full control over shipments, just like SuperUsers, but scoped to the platform's logistics.
    GET_ALL_SHIPMENTS("/internal/api/v1/merchant/shipments/all", "GET"),
    UPDATE_SHIPMENT("/internal/api/v1/merchant/shipments/update", "POST"),
    DELETE_SHIPMENT("/internal/api/v1/merchant/shipments/delete", "DELETE"),

    // "Have the user create shipments labels... Input recipient data"
    CREATE_SHIPMENT("/internal/api/v1/merchant/shipment/create", "POST"),

    // "System generates; QR with just a UUID"
    GENERATE_QR("/internal/api/v1/merchant/qr/generate", "POST"),

    // "analyzeFrame method runs async... caches and persists in background"
    ASYNC_BATCH_SCAN("/internal/api/v1/merchant/scan/batch/async", "POST"),

    // "Re-scan flow: Show the user a list of documents with the errors"
    GET_BATCH_ERRORS("/internal/api/v1/merchant/scan/batch/errors", "GET"),

    // "When the user selects one, a scanner comes up for a re-scan"
    RESOLVE_BATCH_ERROR("/internal/api/v1/merchant/scan/batch/resolve", "POST"),

    // "To complete scanning, there should be a button to complete the scanning process"
    COMPLETE_BATCH_SESSION("/internal/api/v1/merchant/scan/batch/complete", "POST"),

    ROUTES_PAGE("/dashboard/merchant/routes", "GET"),
    CREATE_ROUTE("/internal/api/v1/merchant/routes/create", "POST"),
    UPDATE_ROUTE("/internal/api/v1/merchant/routes/update", "POST"),
    DELETE_ROUTE("/internal/api/v1/merchant/routes/delete", "POST"),

    // Merchant shipment library
    SHIPMENTS_PAGE("/dashboard/merchant/shipments", "GET");

    public static final String SHIPMENTS_PAGE_PATH = "/dashboard/merchant/shipments";

    public static final String GET_ALL_SHIPMENTS_PATH = "/internal/api/v1/merchant/shipments/all";
    public static final String UPDATE_SHIPMENT_PATH = "/internal/api/v1/merchant/shipments/update";
    public static final String CREATE_SHIPMENT_PATH = "/internal/api/v1/merchant/shipment/create";
    public static final String DASHBOARD_PATH = "/dashboard/merchant";
    public static final String CREATE_SHIPMENT_PAGE_PATH = "/dashboard/merchant/create-shipment";
    public static final String SCAN_PAGE_PATH = "/dashboard/merchant/scan";
    public static final String ROUTES_PAGE_PATH = "/dashboard/merchant/routes";
    public static final String CREATE_ROUTE_PATH = "/internal/api/v1/merchant/routes/create";
    public static final String UPDATE_ROUTE_PATH = "/internal/api/v1/merchant/routes/update";
    public static final String DELETE_ROUTE_PATH = "/internal/api/v1/merchant/routes/delete";

    private final String endpoint;
    private final String method;

    MerchantDashboardEndpoints(String endpoint, String method) {
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
