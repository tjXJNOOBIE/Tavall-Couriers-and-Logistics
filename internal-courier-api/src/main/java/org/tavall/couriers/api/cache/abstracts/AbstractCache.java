package org.tavall.couriers.api.cache.abstracts;



import org.tavall.couriers.api.cache.CacheKey;
import org.tavall.couriers.api.cache.CacheValue;
import org.tavall.couriers.api.cache.enums.CacheDomain;
import org.tavall.couriers.api.cache.enums.CacheSource;
import org.tavall.couriers.api.cache.enums.CacheType;
import org.tavall.couriers.api.cache.enums.CacheVersion;
import org.tavall.couriers.api.cache.interfaces.ICacheKey;
import org.tavall.couriers.api.cache.interfaces.ICacheValue;

/**
 * Abstract base class for cache implementations providing common caching functionality
 * @param <K> The key type
 * @param <V> The value type
 */
public abstract class AbstractCache<K, V> implements ICacheKey<K>, ICacheValue<V>{




    protected AbstractCache(){

    }
    @Override
    public CacheType getCacheType() {
        return null;
    }
    @Override
    public CacheDomain getCacheDomain() {

        return null;
    }

    @Override
    public CacheSource getSource() {

        return null;
    }

    @Override
    public CacheVersion getVersion() {

        return null;
    }


    public ICacheKey<?> createKey(K rawKey) {
        return new CacheKey<>(rawKey);
    }
    public ICacheKey<?> createKey(K rawKey, CacheType type) {
        return new CacheKey<>(rawKey, type);
    }

    public ICacheKey<?> createKey(K rawKey, CacheType type, CacheDomain domain) {
        return new CacheKey<>(rawKey, type, domain);
    }

    public ICacheKey<?> createKey(K rawKey, CacheType type, CacheSource source) {
        return new CacheKey<>(rawKey, type, source);
    }

    public ICacheKey<?> createKey(K rawKey, CacheType type, CacheVersion version) {
        return new CacheKey<>(rawKey, type, version);
    }

    public ICacheKey<?> createKey(K rawKey, CacheType type, CacheDomain domain, CacheSource source) {
        return new CacheKey<>(rawKey, type, domain, source);
    }

    public ICacheKey<?> createKey(K rawKey, CacheType type, CacheSource source, CacheVersion version) {
        return new CacheKey<>(rawKey, type, source, version);
    }

    public ICacheKey<?> createKey(K rawKey, CacheType type, CacheDomain domain, CacheVersion version) {
        return new CacheKey<>(rawKey, type, domain, version);
    }

    // Full House
    public ICacheKey<?> createKey(K rawKey, CacheType type, CacheDomain domain, CacheSource source, CacheVersion version) {
        return new CacheKey<>(rawKey, type, domain, source, version);
    }

    // --- 3. THE VALUE WRAPPER (Simple Factory) ---
    protected ICacheValue<V> createValue(V value) {

        return new CacheValue<>(value, System.currentTimeMillis());
    }

}