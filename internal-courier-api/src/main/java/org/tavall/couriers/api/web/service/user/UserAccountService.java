package org.tavall.couriers.api.web.service.user;


import org.tavall.couriers.api.web.user.UserAccount;
import org.tavall.couriers.api.web.user.UserAccountRepository;
import org.tavall.couriers.api.web.user.permission.Role;
import org.tavall.couriers.api.web.user.permission.UserPermissions;

import java.time.Instant;
import java.util.EnumSet;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

public class UserAccountService {


    private final UserAccountRepository repo;

    public UserAccountService(UserAccountRepository repo) {
        this.repo = Objects.requireNonNull(repo, "repo");
    }

    public UserAccount getOrCreateFromOAuthSubject(String subject, String username) {
        Objects.requireNonNull(subject, "subject");
        Objects.requireNonNull(username, "username");

        return repo.findByExternalSubject(subject).orElseGet(() -> {
            UserAccount created = new UserAccount(
                    UUID.randomUUID(),
                    subject,
                    username,
                    true,
                    EnumSet.of(Role.USER),
                    Instant.now()
            );
            return repo.save(created);
        });
    }

    public UserAccount promoteToDriver(UUID actorId, UUID targetUserId) {
        UserAccount actor = require(actorId);
        requirePermission(actor, UserPermissions.USER_PROMOTE_TO_DRIVER);

        UserAccount target = require(targetUserId);
        Set<Role> roles = EnumSet.copyOf(target.getRoles());
        roles.add(Role.DRIVER);

        return repo.save(target.withRoles(roles));
    }

    public UserAccount demoteFromDriver(UUID actorId, UUID targetUserId) {
        UserAccount actor = require(actorId);
        requirePermission(actor, UserPermissions.USER_DEMOTE_FROM_DRIVER);

        UserAccount target = require(targetUserId);
        Set<Role> roles = EnumSet.copyOf(target.getRoles());
        roles.remove(Role.DRIVER);

        return repo.save(target.withRoles(roles));
    }

    private UserAccount require(UUID id) {
        return repo.findById(id).orElseThrow(() -> new IllegalArgumentException("User not found: " + id));
    }

    private void requirePermission(UserAccount actor, UserPermissions permission) {
        if (!actor.hasPermission(permission)) {
            throw new SecurityException("Missing permission: " + permission);
        }
    }
}