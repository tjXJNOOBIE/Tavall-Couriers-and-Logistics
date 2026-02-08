package org.tavall.couriers.api.web.service.user;


import org.springframework.stereotype.Service;
import org.tavall.couriers.api.web.user.UserAccount;
import org.tavall.couriers.api.web.user.UserAccountEntity;
import org.tavall.couriers.api.web.user.UserAccountRepository;
import org.tavall.couriers.api.web.user.cache.UserAccountCache;
import org.tavall.couriers.api.web.user.permission.Role;
import org.tavall.couriers.api.web.user.permission.UserPermissions;

import java.time.Instant;
import java.util.EnumSet;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

@Service
public class UserAccountService {


    private final UserAccountRepository repo;
    private final UserAccountCache userCache;

    public UserAccountService(UserAccountRepository repo, UserAccountCache userCache) {
        this.repo = repo;
        this.userCache = userCache;
    }

    public UserAccountEntity getOrCreateFromOAuthSubject(String subject, String username) {
        Objects.requireNonNull(subject, "subject");
        Objects.requireNonNull(username, "username");

        UserAccount cached = userCache.findByExternalSubject(subject);
        if (cached != null) {
            return toEntity(cached);
        }

        UserAccountEntity existing = repo.findByExternalSubject(subject).orElse(null);
        if (existing != null) {
            userCache.registerUser(toDomain(existing));
            return existing;
        }

        UserAccountEntity created = new UserAccountEntity(
                UUID.randomUUID(),
                subject,
                username,
                true,
                EnumSet.of(Role.USER),
                Instant.now()
        );
        UserAccountEntity saved = repo.save(created);
        userCache.registerUser(toDomain(saved));
        return saved;
    }

    public List<UserAccountEntity> getAllUsers() {
        return repo.findAll();
    }

    public UserAccountEntity createUser(String username, Set<Role> roles) {
        Objects.requireNonNull(username, "username");
        if (username.isBlank()) {
            throw new IllegalArgumentException("Username is required.");
        }

        UserAccount cached = userCache.findByUsername(username);
        if (cached != null) {
            throw new IllegalArgumentException("Username already exists.");
        }

        if (repo.existsByUsernameIgnoreCase(username)) {
            throw new IllegalArgumentException("Username already exists.");
        }

        String normalized = username.trim();
        String externalSubject = "local:" + normalized.toLowerCase(Locale.ROOT);
        if (repo.existsByExternalSubject(externalSubject)) {
            throw new IllegalArgumentException("External subject already exists.");
        }

        Set<Role> assignedRoles = (roles == null || roles.isEmpty())
                ? EnumSet.of(Role.USER)
                : EnumSet.copyOf(roles);

        UserAccountEntity created = new UserAccountEntity(
                UUID.randomUUID(),
                externalSubject,
                normalized,
                true,
                assignedRoles,
                Instant.now()
        );
        UserAccountEntity saved = repo.save(created);
        userCache.registerUser(toDomain(saved));
        return saved;
    }

    public UserAccountEntity promoteToDriver(UUID actorId, UUID targetUserId) {
        UserAccountEntity actor = require(actorId);
        requirePermission(actor, UserPermissions.USER_PROMOTE_TO_DRIVER);

        UserAccountEntity target = require(targetUserId);
        Set<Role> roles = EnumSet.copyOf(target.getRoles());
        roles.add(Role.DRIVER);

        target.setRoles(roles);
        UserAccountEntity saved = repo.save(target);
        userCache.registerUser(toDomain(saved));
        return saved;
    }

    public UserAccountEntity demoteFromDriver(UUID actorId, UUID targetUserId) {
        UserAccountEntity actor = require(actorId);
        requirePermission(actor, UserPermissions.USER_DEMOTE_FROM_DRIVER);

        UserAccountEntity target = require(targetUserId);
        Set<Role> roles = EnumSet.copyOf(target.getRoles());
        roles.remove(Role.DRIVER);

        target.setRoles(roles);
        UserAccountEntity saved = repo.save(target);
        userCache.registerUser(toDomain(saved));
        return saved;
    }

    private UserAccountEntity require(UUID id) {
        UserAccount cached = userCache.findById(id);
        if (cached != null) {
            return toEntity(cached);
        }
        UserAccountEntity found = repo.findById(id).orElseThrow(() -> new IllegalArgumentException("User not found: " + id));
        userCache.registerUser(toDomain(found));
        return found;
    }

    private void requirePermission(UserAccountEntity actor, UserPermissions permission) {
        if (!actor.hasPermission(permission)) {
            throw new SecurityException("Missing permission: " + permission);
        }
    }

    private UserAccount toDomain(UserAccountEntity entity) {
        if (entity == null) {
            return null;
        }
        Set<Role> roles = (entity.getRoles() == null || entity.getRoles().isEmpty())
                ? EnumSet.noneOf(Role.class)
                : EnumSet.copyOf(entity.getRoles());
        return new UserAccount(
                entity.getUserUUID(),
                entity.externalSubject(),
                entity.getUsername(),
                entity.enabled(),
                roles,
                entity.createdAt()
        );
    }

    private UserAccountEntity toEntity(UserAccount account) {
        if (account == null) {
            return null;
        }
        return new UserAccountEntity(
                account.getUserUUID(),
                account.externalSubject(),
                account.getUsername(),
                account.enabled(),
                account.getRoles(),
                account.createdAt()
        );
    }
}
