package org.tavall.couriers.api.cache.maps;


import org.tavall.couriers.api.cache.CacheValue;
import org.tavall.couriers.api.cache.enums.CacheDomain;
import org.tavall.couriers.api.cache.enums.CacheType;
import org.tavall.couriers.api.cache.interfaces.ICacheKey;
import org.tavall.couriers.api.cache.interfaces.ICacheValue;
import org.tavall.couriers.api.console.Log;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

public class CacheMap extends ConcurrentHashMap<ICacheKey<?>, List<ICacheValue<?>>> {

    public static CacheMap INSTANCE = new CacheMap();


    public CacheMap() {
        super();
    }


    public static CacheMap getCacheMap() {

        return INSTANCE;
    }


    public void add(ICacheKey<?> cacheKey, ICacheValue<?> newValue) {

        if (cacheKey != null && newValue != null) {

            compute(cacheKey, (k, bucket) -> {
                if (bucket == null) {
                    bucket = Collections.synchronizedList(new ArrayList<>());
                }

                bucket.add(newValue);
                Log.success("New value added to cache bucket: " + String.valueOf(newValue.getValue()));
                return bucket;
            });

            return;
        }

        Log.error("Error: Cannot add to cache: Key or Value is null.");
    }


    public List<ICacheValue<?>> getBucket(ICacheKey<?> key) {
        if (key == null) return new ArrayList<>();

        List<ICacheValue<?>> bucket = get(key);
        if (bucket == null) return new ArrayList<>();

        // Return a defensive copy to prevent ConcurrentModificationException
        // if the caller iterates while we are adding.
        synchronized (bucket) {
            return new ArrayList<>(bucket);
        }
    }
    /**
     * Type-Safe Extraction helper.
     * You pass the Wrapper you got, and the Class you expect inside it.
     * We handle the ugly casting here so your service code stays clean.
     */
    @SuppressWarnings("unchecked")
    public <T> T unwrap(ICacheValue<?> wrapper, Class<T> type) {
        if (wrapper == null || wrapper.getValue() == null) return null;

        Object rawValue = wrapper.getValue();
        if (type.isInstance(rawValue)) {
            return (T) rawValue;
        }

        Log.error("Cache Type Mismatch. Expected: " + type.getSimpleName() +
                " | Found: " + rawValue.getClass().getSimpleName());
        return null;
    }

    public boolean containsInBucket(Class<?> domainKey, ICacheValue<?> value) {

        List<ICacheValue<?>> bucket = get(domainKey);
        if (domainKey == null) {
            Log.error("Error: domainKey is null");
            return false;
        }
        if (bucket != null) {
            return bucket.contains(value);
        }
        Log.error("Error: Cannot find bucket for domainKey: " + domainKey.toString());
        return false;
    }

// --- REMOVAL METHODS ---

    /**
     * Removes a specific Wrapper value from the bucket, but keeps the Key.
     * This supports your "Key persists, Value eliminated" logic.
     */
    public void removeValue(ICacheValue<?> valueToRemove) {
        if (valueToRemove == null) return;

        // Scan all buckets to find and remove this wrapper instance
        this.forEach((key, bucket) -> {
            synchronized (bucket) {
                if (bucket.remove(valueToRemove)) {
                    Log.info("Removed specific wrapper value from domain: " + key.getCacheDomain());
                }
            }
        });
    }

    /**
     * Hard delete of the Key (and its entire bucket).
     */
    public void removeCacheKey(ICacheKey<?> key) {
        if (key != null) {
            Log.warn("Removing cache Key: " + key.getCacheDomain());
            super.remove(key); // Calls the ConcurrentHashMap remove
        } else {
            Log.error("Cannot remove from cache: Key is null.");
        }
    }


    // --- QUERY HELPERS ---

    public boolean containsDomainKey(ICacheKey<?> key) {
        return containsKey(key);
    }
    /**
     * Checks if the REAL domain object (payload) already exists in the bucket.
     * Unwraps the CacheValue and compares the inner object using .equals().
     */
    public boolean containsPayload(ICacheKey<?> key, Object realPayload) {

        if (key == null || realPayload == null) return false;

        List<ICacheValue<?>> bucket = get(key);
        if (bucket == null) return false;

        synchronized (bucket) {
            for (ICacheValue<?> wrapper : bucket) {
                // Peek inside the wrapper
                Object cachedValue = wrapper.getValue();

                // Use standard Java equality check
                if (realPayload.equals(cachedValue)) {
                    return true;
                }
            }
        }
        return false;
    }
    public List<List<ICacheValue<?>>> findByDomain(CacheDomain domain) {
        return entrySet().stream()
                .filter(entry -> entry.getKey().getCacheDomain() == domain)
                .map(Entry::getValue) // Returns List<ICacheValue<?>>
                .collect(Collectors.toList());
    }

    public List<List<ICacheValue<?>>> findByType(CacheType type) {
        return entrySet().stream()
                .filter(entry -> entry.getKey().getCacheType() == type)
                .map(Entry::getValue)
                .collect(Collectors.toList());
    }


}