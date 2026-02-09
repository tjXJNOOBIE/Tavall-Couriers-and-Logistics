/*
 * TJVD License (TJ Valentineâ€™s Discretionary License) â€” Version 1.0 (2025)
 *
 * Copyright (c) 2025 Taheesh Valentine
 *
 * This source code is protected under the TJVD License.
 * SEE LICENSE.TXT
 */

package org.tavall.couriers.api.web.endpoints.page;

import org.tavall.couriers.api.web.endpoints.AppEndpoint;

public enum PageViewEndpoints implements AppEndpoint {
    HOME("/", "GET"),
    LOGIN("/login", "GET"),
    TRACKING("/tracking", "GET"),
    TRACKING_DETAIL("/tracking/{trackingNumber}", "GET"),
    PURCHASE("/purchase", "GET");

    public static final String HOME_PATH = "/";
    public static final String LOGIN_PATH = "/login";
    public static final String TRACKING_PATH = "/tracking";
    public static final String TRACKING_DETAIL_PATH = "/tracking/{trackingNumber}";
    public static final String PURCHASE_PATH = "/purchase";

    private final String endpoint;
    private final String method;

    PageViewEndpoints(String endpoint, String method) {
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
