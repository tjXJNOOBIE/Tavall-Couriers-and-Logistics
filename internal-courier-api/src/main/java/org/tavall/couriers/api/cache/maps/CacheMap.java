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

    public static final CacheMap INSTANCE = new CacheMap();


    private CacheMap() {
        super();
    }

    public void add(ICacheKey<?> cacheKey, ICacheValue<?> newValue) {

        if (cacheKey != null && newValue != null) {
            this.compute(cacheKey, (k, cacheValues) -> {

                List<ICacheValue<?>> valueList;
                // If null, make new List. If exists, cast the existing value to List.
                if (cacheValues != null) {
                    valueList = cacheValues;
                    valueList.add(newValue);

                    return valueList;
                }
                Log.error("Error: ScanResponse not found in cache. Key: " + cacheKey);


                // Returns the List (which map stores as the Value)
                return null; // Assuming Map allows raw List or you wrap this List in a CacheValue
            });
            return;
        }
        Log.error("Error: Cannot add to cache: Key or Value is null.");
    }
    public void removeByValue(ICacheValue<?> valueToRemove) {

        if (valueToRemove != null) {

            // Iterate all entries (Keys -> Lists)
            this.entrySet().removeIf(entry -> {

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
        return this.entrySet().stream()
                .filter(entry -> entry.getKey().getCacheDomain() == domain)
                .map(Entry::getValue)
                .collect(Collectors.toList());
    }
    public List<List<ICacheValue<?>>> findByType(CacheType type) {
        return this.entrySet().stream()
                .filter(entry -> entry.getKey().getCacheType() == type)
                .map(Entry::getValue)
                .collect(Collectors.toList());
    }






}