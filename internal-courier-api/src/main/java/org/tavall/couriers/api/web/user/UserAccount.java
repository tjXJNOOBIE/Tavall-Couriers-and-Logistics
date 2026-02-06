package org.tavall.couriers.api.web.user;



import org.tavall.couriers.api.web.user.permission.Role;
import org.tavall.couriers.api.web.user.permission.UserPermissions;

import java.security.Permission;
import java.time.Instant;
import java.util.*;

public class UserAccount {

    private final UUID id;
    private final String externalSubject; // OAuth2 "sub"
    private final String username;
    private final boolean enabled;
    private final Set<Role> roles;
    private final Instant createdAt;

    public UserAccount(UUID id,
                       String externalSubject,
                       String username,
                       boolean enabled,
                       Set<Role> roles,
                       Instant createdAt) {
        this.id = Objects.requireNonNull(id, "id");
        this.externalSubject = Objects.requireNonNull(externalSubject, "externalSubject");
        this.username = Objects.requireNonNull(username, "username");
        this.enabled = enabled;
        this.roles = Collections.unmodifiableSet(EnumSet.copyOf(Objects.requireNonNull(roles, "roles")));
        this.createdAt = Objects.requireNonNull(createdAt, "createdAt");
    }

    public UUID getUserUUID() { return id; }
    public String externalSubject() { return externalSubject; }
    public String getUsername() { return username; }
    public boolean enabled() { return enabled; }
    public Set<Role> getRoles() { return roles; }
    public Instant createdAt() { return createdAt; }

    public Set<UserPermissions> permissions() {
        EnumSet<UserPermissions> perms = EnumSet.noneOf(UserPermissions.class);
        for (Role role : roles) {
            perms.addAll(role.permissions());
        }
        return Collections.unmodifiableSet(perms);
    }

    public boolean hasPermission(UserPermissions permission) {
        return permissions().contains(permission);
    }

    public UserAccount withRoles(Set<Role> newRoles) {
        return new UserAccount(id, externalSubject, username, enabled, newRoles, createdAt);
    }

    public UserAccount disabled() {
        return new UserAccount(id, externalSubject, username, false, roles, createdAt);
    }
}