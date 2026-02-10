package org.tavall.couriers.api.web.user;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Table;
import org.tavall.couriers.api.web.user.permission.Role;
import org.tavall.couriers.api.web.user.permission.UserPermissions;

import java.time.Instant;
import java.util.Collections;
import java.util.EnumSet;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

@Entity
@Table(name = "user_accounts", schema = "courier_schemas")
public class UserAccountEntity {

    @Id
    @Column(name = "id", nullable = false, columnDefinition = "uuid")
    private UUID id;

    @Column(name = "external_subject", nullable = false, length = 160, unique = true)
    private String externalSubject; // OAuth2 "sub"

    @Column(name = "username", nullable = false, length = 120, unique = true)
    private String username;

    @Column(name = "enabled", nullable = false)
    private boolean enabled;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(
            name = "user_account_roles",
            schema = "courier_schemas",
            joinColumns = @JoinColumn(name = "user_id")
    )
    @Column(name = "role", nullable = false, length = 40)
    @Enumerated(EnumType.STRING)
    private Set<Role> roles = EnumSet.noneOf(Role.class);

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected UserAccountEntity() {
    }

    public UserAccountEntity(UUID id,
                             String externalSubject,
                             String username,
                             boolean enabled,
                             Set<Role> roles,
                             Instant createdAt) {
        this.id = Objects.requireNonNull(id, "id");
        this.externalSubject = Objects.requireNonNull(externalSubject, "externalSubject");
        this.username = Objects.requireNonNull(username, "username");
        this.enabled = enabled;
        setRoles(roles);
        this.createdAt = Objects.requireNonNull(createdAt, "createdAt");
    }

    public UUID getUserUUID() { return id; }
    public void setUserUUID(UUID id) { this.id = id; }

    public String externalSubject() { return externalSubject; }
    public void setExternalSubject(String externalSubject) { this.externalSubject = externalSubject; }

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }

    public boolean enabled() { return enabled; }
    public void setEnabled(boolean enabled) { this.enabled = enabled; }

    public Set<Role> getRoles() { return roles; }
    public void setRoles(Set<Role> roles) {
        if (roles == null || roles.isEmpty()) {
            this.roles = EnumSet.noneOf(Role.class);
        } else {
            this.roles = EnumSet.copyOf(roles);
        }
    }

    public Instant createdAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }

    public Set<UserPermissions> permissions() {
        EnumSet<UserPermissions> perms = EnumSet.noneOf(UserPermissions.class);
        if (roles != null) {
            for (Role role : roles) {
                perms.addAll(role.permissions());
            }
        }
        return Collections.unmodifiableSet(perms);
    }

    public boolean hasPermission(UserPermissions permission) {
        return permissions().contains(permission);
    }

    public UserAccountEntity withRoles(Set<Role> newRoles) {
        return new UserAccountEntity(id, externalSubject, username, enabled, newRoles, createdAt);
    }

    public UserAccountEntity disabled() {
        return new UserAccountEntity(id, externalSubject, username, false, roles, createdAt);
    }
}
