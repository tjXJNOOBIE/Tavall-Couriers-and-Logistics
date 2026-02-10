package org.tavall.couriers.api.web.user.cache;

import org.springframework.stereotype.Component;
import org.tavall.couriers.api.cache.abstracts.AbstractCache;
import org.tavall.couriers.api.cache.enums.CacheDomain;
import org.tavall.couriers.api.cache.enums.CacheSource;
import org.tavall.couriers.api.cache.enums.CacheType;
import org.tavall.couriers.api.cache.enums.CacheVersion;
import org.tavall.couriers.api.cache.interfaces.ICacheKey;
import org.tavall.couriers.api.cache.interfaces.ICacheValue;
import org.tavall.couriers.api.cache.maps.CacheMap;
import org.tavall.couriers.api.console.Log;
import org.tavall.couriers.api.web.user.UserAccount;

import java.util.List;
import java.util.UUID;

@Component
public class UserAccountCache extends AbstractCache<UserAccountCache, UserAccount> {

    private ICacheKey<UserAccountCache> cacheKey;
    private ICacheValue<?> cacheValue;
    private volatile boolean primed;

    public UserAccountCache() {
        super();
    }

    @Override
    public CacheType getCacheType() {
        return CacheType.MEMORY;
    }

    @Override
    public CacheDomain getCacheDomain() {
        return CacheDomain.USER;
    }

    @Override
    public CacheSource getSource() {
        return CacheSource.USER_ACCOUNT_SERVICE;
    }

    @Override
    public CacheVersion getVersion() {
        return CacheVersion.V1_0;
    }

    @SuppressWarnings("unchecked")
    public void registerUser(UserAccount account) {
        if (account == null) {
            Log.error("Error: Cannot register null user account.");
            return;
        }

        ensureCacheKey();
        this.cacheValue = createValue(account);
        CacheMap.getCacheMap().add(cacheKey, cacheValue);
        primed = true;
        Log.success("User account registered in cache: " + account.getUsername());
    }

    public void primeCache(List<UserAccount> accounts) {
        ensureCacheKey();
        if (accounts == null) {
            primed = true;
            return;
        }
        for (UserAccount account : accounts) {
            if (account == null) {
                continue;
            }
            this.cacheValue = createValue(account);
            CacheMap.getCacheMap().add(cacheKey, cacheValue);
        }
        primed = true;
        Log.success("User account cache primed with " + accounts.size() + " users.");
    }

    public List<UserAccount> getAllUsers() {
        ensureCacheKey();
        List<ICacheValue<?>> bucket = CacheMap.getCacheMap().getBucket(cacheKey);
        if (bucket.isEmpty()) {
            return List.of();
        }
        List<UserAccount> users = new java.util.ArrayList<>();
        for (ICacheValue<?> wrapper : bucket) {
            UserAccount value = CacheMap.getCacheMap().unwrap(wrapper, UserAccount.class);
            if (value != null) {
                users.add(value);
            }
        }
        return users;
    }

    public boolean isPrimed() {
        return primed;
    }

    public UserAccount findById(UUID id) {
        if (id == null) return null;
        return findFirstMatch(user -> id.equals(user.getUserUUID()), "id=" + id);
    }

    public UserAccount findByExternalSubject(String subject) {
        if (subject == null || subject.isBlank()) return null;
        return findFirstMatch(user -> subject.equals(user.externalSubject()), "subject=" + subject);
    }

    public UserAccount findByUsername(String username) {
        if (username == null || username.isBlank()) return null;
        return findFirstMatch(user -> username.equalsIgnoreCase(user.getUsername()), "username=" + username);
    }

    public boolean containsUserKey() {
        ensureCacheKey();
        boolean contains = CacheMap.getCacheMap().containsKey(this.cacheKey);
        Log.info("User account cache key present: " + contains);
        return contains;
    }

    public void removeUser(UserAccount account) {
        if (account == null) return;
        ensureCacheKey();
        CacheMap.getCacheMap().removeValue(createValue(account));
        Log.info("User account removed from cache: " + account.getUsername());
    }



    private UserAccount findFirstMatch(UserPredicate predicate, String hint) {
        ensureCacheKey();
        List<ICacheValue<?>> bucket = CacheMap.getCacheMap().getBucket(cacheKey);
        if (bucket.isEmpty()) {
            Log.info("User account cache empty for lookup: " + hint);
            return null;
        }

        for (ICacheValue<?> wrapper : bucket) {
            UserAccount value = CacheMap.getCacheMap().unwrap(wrapper, UserAccount.class);
            if (value != null && predicate.matches(value)) {
                Log.info("User account cache hit: " + hint);
                return value;
            }
        }

        Log.info("User account cache miss: " + hint);
        return null;
    }

    @SuppressWarnings("unchecked")
    private void ensureCacheKey() {
        if (this.cacheKey == null) {
            this.cacheKey = (ICacheKey<UserAccountCache>) createKey(
                    this,
                    CacheType.MEMORY,
                    CacheDomain.USER,
                    CacheSource.USER_ACCOUNT_SERVICE,
                    CacheVersion.V1_0
            );
        }
    }

    private interface UserPredicate {
        boolean matches(UserAccount user);
    }
}
