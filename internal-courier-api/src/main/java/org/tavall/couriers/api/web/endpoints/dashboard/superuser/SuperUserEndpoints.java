/*
 * TJVD License (TJ Valentine’s Discretionary License) — Version 1.0 (2025)
 *
 * Copyright (c) 2025 Taheesh Valentine
 *
 * This source code is protected under the TJVD License.
 * SEE LICENSE.TXT
 */

package org.tavall.couriers.api.web.endpoints.dashboard.superuser;

import org.tavall.couriers.api.web.endpoints.AppEndpoint;

public enum SuperUserEndpoints implements AppEndpoint {

    // --- 1. SHIPMENT LABELS ("view, edit, or destroy any/all") ---
    GET_ALL_SHIPMENTS("/internal/api/v1/admin/shipments/all", "GET"),
    FORCE_UPDATE_SHIPMENT("/internal/api/v1/admin/shipments/update", "POST"), // Bypasses state checks
    DESTROY_SHIPMENT("/internal/api/v1/admin/shipments/delete", "DELETE"),

    // --- 2. USERS & ROLES ("view, edit, or destroy any/all") ---
    GET_ALL_USERS("/internal/api/v1/admin/users/all", "GET"),
    FORCE_UPDATE_USER_ROLE("/internal/api/v1/admin/users/role/update", "POST"),
    FORCE_DELETE_USER("/internal/api/v1/admin/users/delete/force", "DELETE"),

    // --- 3. DELIVERY STATUSES ("view, edit, or destroy any/all") ---
    // This allows modifying the "DeliveryStateManager" rules dynamically or fixing stuck states
    GET_ALL_STATUS_DEFINITIONS("/internal/api/v1/admin/status/definitions", "GET"),
    CREATE_OR_UPDATE_STATUS("/internal/api/v1/admin/status/update", "POST"),
    DELETE_STATUS_DEFINITION("/internal/api/v1/admin/status/delete", "DELETE");

    public static final String GET_ALL_SHIPMENTS_PATH = "/internal/api/v1/admin/shipments/all";
    public static final String FORCE_UPDATE_SHIPMENT_PATH = "/internal/api/v1/admin/shipments/update";
    public static final String DASHBOARD_PATH = "/superuser/dashboard";
    public static final String CREATE_SHIPMENT_PAGE_PATH = "/superuser/create-shipment";

    private final String endpoint;
    private final String method;

    SuperUserEndpoints(String endpoint, String method) {
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
