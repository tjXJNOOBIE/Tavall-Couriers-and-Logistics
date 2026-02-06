/*
 * TJVD License (TJ Valentine’s Discretionary License) — Version 1.0 (2025)
 *
 * Copyright (c) 2025 Taheesh Valentine
 *
 * This source code is protected under the TJVD License.
 * SEE LICENSE.TXT
 */

package org.tavall.couriers.api.web.user.permission;


import java.util.Collections;
import java.util.EnumSet;
import java.util.Set;

public enum Role {


    USER(EnumSet.noneOf(UserPermissions.class)),

    DRIVER(EnumSet.of(
            UserPermissions.DELIVERY_TRANSITION_BASIC,
            UserPermissions.SHIPMENT_VIEW,
            UserPermissions.USER_VIEW_SELF)),

    MERCHANT(EnumSet.of(
            UserPermissions.SHIPMENT_CREATE,
            UserPermissions.SHIPMENT_VIEW,
            UserPermissions.SHIPMENT_EDIT,
            UserPermissions.USER_PROMOTE_TO_DRIVER,
            UserPermissions.USER_DEMOTE_FROM_DRIVER)),

    SUPERUSER(EnumSet.allOf(UserPermissions.class)),

    SUPPORT(EnumSet.of(
            UserPermissions.ADMIN_VIEW_USERS,
            UserPermissions.ADMIN_VIEW_SHIPMENTS)),

    CUSTOMER(EnumSet.of(
            UserPermissions.SHIPMENT_VIEW)),

    SYSTEM(EnumSet.of(
            UserPermissions.SYSTEM_SCHEDULE_JOBS,
            UserPermissions.SYSTEM_MONITOR));

    private final Set<UserPermissions> permissions;

    Role(Set<UserPermissions> permissions) {
        this.permissions = Collections.unmodifiableSet(permissions);
    }

    public Set<UserPermissions> permissions() {
        return permissions;
    }
}