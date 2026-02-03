package org.tavall.couriers.api.cache.maps;


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

    public boolean containsDomainKey(ICacheKey<?> key) {
        return containsKey(key);
    }
    public List<ICacheValue<?>> bucket(Class<?> domainKey) {
        return getOrDefault(domainKey, List.of());
    }

    public boolean containsInBucket(Class<?> domainKey, ICacheValue<?> value) {
        List<ICacheValue<?>> bucket = get(domainKey);
        if(domainKey == null){
            Log.error("Error: domainKey is null");
            return false;
        }
        if(bucket != null){
            return bucket.contains(value);
        }
        Log.error("Error: Cannot find bucket for domainKey: " + domainKey.toString());
        return false;
    }
    public void removeByValue(ICacheValue<?> valueToRemove) {

        if (valueToRemove != null) {

            // Iterate all entries (Keys -> Lists)
            entrySet().removeIf(entry -> {

                List<ICacheValue<?>> list = entry.getValue();

                // Attempt to remove the item from this specific list
                // Relies on valueToRemove.equals() being implemented correctly!
                boolean wasRemoved = list.remove(valueToRemove);

                if (wasRemoved) {
                    Log.info("Removed value from cache bucket: " + entry.getKey().getCacheDomain());
                }

                // CLEANUP: If the list is now empty, return true.
                // This tells the Map to delete the Key entirely.
                return list.isEmpty();
            });

            return;
        }

        Log.error("Error: Cannot remove null value from cache.");
    }
    public List<ICacheValue<?>> remove(ICacheKey<?> key) {
        if (key != null) {

            Log.warn("Removing cache entry: " + key.getCacheDomain());
            return super.remove(key); // Call SUPER, not self.
        }
        Log.error("Cannot remove from cache: Key is null.");
        return null; // Call SUPER, not self.
    }
    public List<List<ICacheValue<?>>> findByDomain(CacheDomain domain) {
        return entrySet().stream()
                .filter(entry -> entry.getKey().getCacheDomain() == domain)
                .map(Entry::getValue)
                .collect(Collectors.toList());
    }
    public List<List<ICacheValue<?>>> findByType(CacheType type) {
        return entrySet().stream()
                .filter(entry -> entry.getKey().getCacheType() == type)
                .map(Entry::getValue)
                .collect(Collectors.toList());
    }






}