/*
 * TJVD License (TJ Valentine’s Discretionary License) — Version 1.0 (2025)
 *
 * Copyright (c) 2025 Taheesh Valentine
 *
 * This source code is protected under the TJVD License.
 * SEE LICENSE.TXT
 */

package org.tavall.couriers.api.web.user.permission;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

public enum UserPermissions {
    AUTH_LOGIN,

    USER_VIEW_SELF,
    USER_EDIT_SELF,

    USER_PROMOTE_TO_DRIVER,
    USER_DEMOTE_FROM_DRIVER,

    SHIPMENT_CREATE,
    SHIPMENT_VIEW,
    SHIPMENT_EDIT,
    SHIPMENT_DELETE,
    MERCHANT_INTAKE_SCAN,

    ROUTE_CREATE,
    ROUTE_VIEW,
    ROUTE_EDIT,
    ROUTE_DELETE,

    DELIVERY_TRANSITION_BASIC,
    DELIVERY_TRANSITION_ANY,

    ADMIN_VIEW_USERS,
    ADMIN_EDIT_USERS,
    ADMIN_DELETE_USERS,

    ADMIN_VIEW_SHIPMENTS,
    ADMIN_EDIT_SHIPMENTS,
    ADMIN_DELETE_SHIPMENTS,

    SYSTEM_SCHEDULE_JOBS,
    SYSTEM_MONITOR;

    public static final String PREFIX = "PERM_";

    public String authority() {
        return PREFIX + name();
    }

    public GrantedAuthority grantedAuthority() {
        return new SimpleGrantedAuthority(authority());
    }

}
