package org.tavall.couriers.api.web.endpoints.shipping;

import org.tavall.couriers.api.web.endpoints.AppEndpoint;

public enum ShippingLabelEndpoints implements AppEndpoint {

    SHIPPING_LABELS("/shipping-labels", "GET"),
    SHIPPING_LABEL_DETAIL("/shipping-labels/{uuid}", "GET");

    public static final String SHIPPING_LABELS_PATH = "/shipping-labels";
    public static final String SHIPPING_LABEL_DETAIL_PATH = "/shipping-labels/{uuid}";

    private final String endpoint;
    private final String method;

    ShippingLabelEndpoints(String endpoint, String method) {
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