package org.tavall.couriers.api.web.service.user;


import jakarta.annotation.PostConstruct;
import org.springframework.stereotype.Service;
import org.tavall.couriers.api.concurrent.AsyncTask;
import org.tavall.couriers.api.console.Log;
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
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;

@Service
public class UserAccountService {

    private static final Map<String, UUID> DEFAULT_USER_IDS = Map.of(
            "driver", UUID.fromString("11111111-1111-1111-1111-111111111111"),
            "merchant", UUID.fromString("22222222-2222-2222-2222-222222222222"),
            "superuser", UUID.fromString("33333333-3333-3333-3333-333333333333"),
            "user", UUID.fromString("44444444-4444-4444-4444-444444444444")
    );

    private static final Map<String, Set<Role>> DEFAULT_USER_ROLES = Map.of(
            "driver", EnumSet.of(Role.DRIVER),
            "merchant", EnumSet.of(Role.MERCHANT),
            "superuser", EnumSet.of(Role.SUPERUSER),
            "user", EnumSet.of(Role.USER)
    );

    private final UserAccountRepository repo;
    private final UserAccountCache userCache;
    private final AtomicBoolean priming = new AtomicBoolean(false);

    public UserAccountService(UserAccountRepository repo, UserAccountCache userCache) {
        this.repo = repo;
        this.userCache = userCache;
    }

    @PostConstruct
    public void warmCaches() {
        primeCacheFromDatabase();
    }

    public UserAccountEntity getOrCreateFromOAuthSubject(String subject, String username) {
        Objects.requireNonNull(subject, "subject");
        Objects.requireNonNull(username, "username");

        UserAccount cached = userCache.findByExternalSubject(subject);
        if (cached != null) {
            Log.info("User cache hit for subject: " + subject);
            return toEntity(cached);
        }

        UserAccountEntity existing = repo.findByExternalSubject(subject).orElse(null);
        if (existing != null) {
            Log.info("User loaded from database: " + existing.getUsername());
            userCache.registerUser(toDomain(existing));
            return existing;
        }

        UserAccountEntity created = new UserAccountEntity(
                resolveDefaultId(username),
                subject,
                username,
                true,
                resolveDefaultRoles(username),
                Instant.now()
        );
        persistAsync(created);
        Log.success("User account created: " + created.getUsername());
        userCache.registerUser(toDomain(created));
        return created;
    }

    public List<UserAccountEntity> getAllUsers() {
        List<UserAccount> cached = userCache.getAllUsers();
        if (!cached.isEmpty() || userCache.isPrimed()) {
            return cached.stream().map(this::toEntity).toList();
        }
        primeCacheAsync();
        Log.info("User cache priming in background; returning cached users.");
        return List.of();
    }

    public UserAccountEntity findByUsername(String username) {
        if (username == null || username.isBlank()) {
            return null;
        }
        UserAccount cached = userCache.findByUsername(username);
        if (cached != null || userCache.isPrimed()) {
            return toEntity(cached);
        }
        primeCacheAsync();
        Log.info("User cache priming in background; username lookup deferred: " + username);
        return null;
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
        persistAsync(created);
        Log.success("User account created: " + created.getUsername());
        userCache.registerUser(toDomain(created));
        return created;
    }

    public UserAccountEntity promoteToDriver(UUID actorId, UUID targetUserId) {
        UserAccountEntity actor = require(actorId);
        requirePermission(actor, UserPermissions.USER_PROMOTE_TO_DRIVER);

        UserAccountEntity target = require(targetUserId);
        Set<Role> roles = EnumSet.copyOf(target.getRoles());
        roles.add(Role.DRIVER);

        target.setRoles(roles);
        persistAsync(target);
        Log.success("User promoted to driver: " + target.getUsername());
        userCache.registerUser(toDomain(target));
        return target;
    }

    public UserAccountEntity demoteFromDriver(UUID actorId, UUID targetUserId) {
        UserAccountEntity actor = require(actorId);
        requirePermission(actor, UserPermissions.USER_DEMOTE_FROM_DRIVER);

        UserAccountEntity target = require(targetUserId);
        Set<Role> roles = EnumSet.copyOf(target.getRoles());
        roles.remove(Role.DRIVER);

        target.setRoles(roles);
        persistAsync(target);
        Log.success("User demoted from driver: " + target.getUsername());
        userCache.registerUser(toDomain(target));
        return target;
    }

    public UserAccountEntity deleteUser(UUID actorId, UUID targetUserId) {
        UserAccountEntity actor = require(actorId);
        requirePermission(actor, UserPermissions.ADMIN_DELETE_USERS);

        UserAccountEntity target = require(targetUserId);
        if (actorId != null && actorId.equals(targetUserId)) {
            throw new IllegalArgumentException("Cannot delete the active user.");
        }

        repo.deleteById(targetUserId);
        UserAccount cached = userCache.findById(targetUserId);
        if (cached != null) {
            userCache.removeUser(cached);
        }
        Log.success("User deleted: " + target.getUsername());
        return target;
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

    public void flushUser(String username) {
        if (username == null || username.isBlank()) {
            return;
        }
        UserAccount cached = userCache.findByUsername(username);
        if (cached == null) {
            return;
        }
        persistAsync(toEntity(cached));
        userCache.removeUser(cached);
        Log.info("User cache flushed: " + username);
    }

    private void primeCacheFromDatabase() {
        try {
            List<UserAccountEntity> users = repo.findAll();
            List<UserAccount> domains = users.stream().map(this::toDomain).toList();
            userCache.primeCache(domains);
            Log.success("User account cache primed from database.");
        } catch (Exception ex) {
            Log.warn("Unable to prime user cache: " + ex.getMessage());
        }
    }

    private void primeCacheAsync() {
        if (!priming.compareAndSet(false, true)) {
            return;
        }
        AsyncTask.runFuture(() -> {
            try {
                primeCacheFromDatabase();
            } finally {
                priming.set(false);
            }
            return null;
        });
    }

    private void persistAsync(UserAccountEntity entity) {
        if (entity == null) {
            return;
        }
        AsyncTask.runFuture(() -> {
            repo.save(entity);
            Log.info("User account persisted async: " + entity.getUsername());
            return null;
        });
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

    private UUID resolveDefaultId(String username) {
        if (username == null) {
            return UUID.randomUUID();
        }
        String key = username.trim().toLowerCase(Locale.ROOT);
        return DEFAULT_USER_IDS.getOrDefault(key, UUID.randomUUID());
    }

    private Set<Role> resolveDefaultRoles(String username) {
        if (username == null) {
            return EnumSet.of(Role.USER);
        }
        String key = username.trim().toLowerCase(Locale.ROOT);
        Set<Role> roles = DEFAULT_USER_ROLES.get(key);
        return roles == null ? EnumSet.of(Role.USER) : EnumSet.copyOf(roles);
    }
}
