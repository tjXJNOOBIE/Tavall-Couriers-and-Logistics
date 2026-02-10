/*
 * TJVD License (TJ Valentine’s Discretionary License) — Version 1.0 (2025)
 *
 * Copyright (c) 2025 Taheesh Valentine
 *
 * This source code is protected under the TJVD License.
 * SEE LICENSE.TXT
 */

package org.tavall.couriers.api.web.endpoints.user;

import org.tavall.couriers.api.web.endpoints.AppEndpoint;

public enum UserAuthEndpoints implements AppEndpoint {
    // --- ACCESS CONTROL ---
    LOGIN("/internal/api/v1/auth/login", "POST"),
    LOGOUT("/internal/api/v1/auth/logout", "POST"),
    CHECK_SESSION("/internal/api/v1/auth/session", "GET"),

    // --- USER MUTATIONS  ---
    // "The main promotion and demotion are handled by UserAccountService.java"
    PROMOTE_USER("/internal/api/v1/users/promote", "POST"), // Moves User -> Driver
    DEMOTE_USER("/internal/api/v1/users/demote", "POST"),   // Moves Driver -> User

    // "Can BE Deleted?: YES"
    DELETE_USER("/internal/api/v1/users/delete", "DELETE"),

    // "Merchant accounts must be created by a Merchant, Superuser, or System"
    CREATE_MERCHANT("/internal/api/v1/users/merchant/create", "POST"),

    // "Merchant accounts can only be frozen, disabled, or deleted"
    DISABLE_USER("/internal/api/v1/users/status/disable", "POST");

    public static final String LOGIN_PATH = "/internal/api/v1/auth/login";
    public static final String LOGOUT_PATH = "/internal/api/v1/auth/logout";
    public static final String CHECK_SESSION_PATH = "/internal/api/v1/auth/session";
    public static final String PROMOTE_USER_PATH = "/internal/api/v1/users/promote";
    public static final String DEMOTE_USER_PATH = "/internal/api/v1/users/demote";
    public static final String DELETE_USER_PATH = "/internal/api/v1/users/delete";
    public static final String CREATE_MERCHANT_PATH = "/internal/api/v1/users/merchant/create";
    public static final String DISABLE_USER_PATH = "/internal/api/v1/users/status/disable";

    private final String endpoint;
    private final String method;

    UserAuthEndpoints(String endpoint, String method) {
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
