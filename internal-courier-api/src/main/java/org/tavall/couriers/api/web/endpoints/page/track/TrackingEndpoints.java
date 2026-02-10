/*
 * TJVD License (TJ Valentine’s Discretionary License) — Version 1.0 (2025)
 *
 * Copyright (c) 2025 Taheesh Valentine
 *
 * This source code is protected under the TJVD License.
 * SEE LICENSE.TXT
 */

package org.tavall.couriers.api.web.endpoints.page.track;

import org.tavall.couriers.api.web.endpoints.AppEndpoint;

public enum TrackingEndpoints implements AppEndpoint {

    // "Live match when the user completes typing/pasting"
    SEARCH_TRACKING("/internal/api/v1/tracking/search", "GET"),

    // "Option to track multiple shipments, make this a list view"
    BATCH_TRACKING("/internal/api/v1/tracking/batch", "POST"),

    // "Display the shipping label data... status... date/time"
    GET_SHIPMENT_DETAILS("/internal/api/v1/tracking/details", "GET");

    private final String endpoint;
    private final String method;

    TrackingEndpoints(String endpoint, String method) {
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