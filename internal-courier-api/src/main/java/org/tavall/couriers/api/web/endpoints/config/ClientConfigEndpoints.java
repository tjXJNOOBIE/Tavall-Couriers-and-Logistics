/*
 * TJVD License (TJ Valentineâ€™s Discretionary License) â€” Version 1.0 (2025)
 *
 * Copyright (c) 2025 Taheesh Valentine
 *
 * This source code is protected under the TJVD License.
 * SEE LICENSE.TXT
 */

package org.tavall.couriers.api.web.endpoints.config;

import org.tavall.couriers.api.web.endpoints.AppEndpoint;

public enum ClientConfigEndpoints implements AppEndpoint {
    HANDSHAKE("/internal/api/v1/config/handshake", "GET");

    public static final String HANDSHAKE_PATH = "/internal/api/v1/config/handshake";

    private final String endpoint;
    private final String method;

    ClientConfigEndpoints(String endpoint, String method) {
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
