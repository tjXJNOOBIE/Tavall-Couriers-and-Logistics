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

import java.util.*;

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
            UserPermissions.MERCHANT_INTAKE_SCAN,
            UserPermissions.ROUTE_CREATE,
            UserPermissions.ROUTE_VIEW,
            UserPermissions.ROUTE_EDIT,
            UserPermissions.ROUTE_DELETE,
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

    public static final String PREFIX = "ROLE_";

    private final Set<UserPermissions> permissions;

    Role(Set<UserPermissions> permissions) {
        this.permissions = Collections.unmodifiableSet(permissions);
    }

    public Set<UserPermissions> permissions() {
        return permissions;
    }

    public String authority() {
        return PREFIX + name();
    }

    public GrantedAuthority grantedAuthority() {
        return new SimpleGrantedAuthority(authority());
    }

    public Collection<? extends GrantedAuthority> grantedAuthorities() {
        List<GrantedAuthority> out = new ArrayList<>();
        out.add(grantedAuthority());
        for (UserPermissions perm : permissions) {
            out.add(perm.grantedAuthority());
        }
        return Collections.unmodifiableList(out);
    }
}
