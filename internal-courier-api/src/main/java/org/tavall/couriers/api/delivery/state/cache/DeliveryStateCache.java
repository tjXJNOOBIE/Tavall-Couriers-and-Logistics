package org.tavall.couriers.api.delivery.state.cache;


import org.tavall.couriers.api.cache.abstracts.AbstractCache;
import org.tavall.couriers.api.cache.enums.CacheDomain;
import org.tavall.couriers.api.cache.enums.CacheSource;
import org.tavall.couriers.api.cache.enums.CacheType;
import org.tavall.couriers.api.cache.enums.CacheVersion;
import org.tavall.couriers.api.cache.interfaces.ICacheKey;
import org.tavall.couriers.api.cache.interfaces.ICacheValue;
import org.tavall.couriers.api.cache.maps.CacheMap;
import org.tavall.couriers.api.concurrent.AsyncTask;
import org.tavall.couriers.api.console.Log;
import org.tavall.couriers.api.shipping.ShippingLabelMetaData;

import java.util.concurrent.Callable;
import java.util.concurrent.StructuredTaskScope;

public class DeliveryStateCache extends AbstractCache<DeliveryStateCache, ShippingLabelMetaData> {

    // If you don't bind it, it crashes. This is a feature, not a bug.
    public static final ScopedValue<DeliveryStateCache> CURRENT = ScopedValue.newInstance();
    private ICacheKey<DeliveryStateCache> cacheKey;
    private ICacheValue<?> cacheValue;
    //TODO: Make ICacheValue = ICacheValue<SHippingLabelMetaData>; same for other caches
    public DeliveryStateCache() {
        super();
    }

    // --- Strict Accessor ---

    public static DeliveryStateCache get() {
        if (!CURRENT.isBound()) {
            throw new IllegalStateException("CRITICAL: DeliveryStateCache accessed outside of a 'runAsync' scope. You must wrap your execution.");
        }
        return CURRENT.get();
    }

    // --- Execution Helper (Async + Scope) ---

    public static <T> T runAsync(ShippingLabelMetaData initialData, Callable<T> task)
            throws InterruptedException, StructuredTaskScope.TimeoutException, StructuredTaskScope.FailedException, Exception {

        DeliveryStateCache cache = new DeliveryStateCache();

        // Bind the scope -> Execute
        return ScopedValue.where(CURRENT, cache).call(() -> {

            // Seed the cache with the label data if provided
            if (initialData != null) {
                DeliveryStateCache.get().registerDeliveryState(initialData);
            }

            // Delegate to your AsyncTask lib (inherits the scope automatically)
            return AsyncTask.runAsync(task);
        });
    }

    // --- Cache Implementation ---

    @Override
    public CacheType getCacheType() { return CacheType.MEMORY; }

    @Override
    public CacheDomain getCacheDomain() { return CacheDomain.DELIVERY; }

    @Override
    public CacheSource getSource() { return CacheSource.DELIVERY_STATE_TRACKER; }

    @Override
    public CacheVersion getVersion() { return CacheVersion.V1_0; }

    @SuppressWarnings("unchecked")
    public void registerDeliveryState(ShippingLabelMetaData labelData) {
        if (labelData != null) {
            // Create key referencing 'this' instance
            this.cacheKey = (ICacheKey<DeliveryStateCache>) createKey(
                    this,
                    CacheType.MEMORY,
                    CacheDomain.DELIVERY,
                    CacheSource.DELIVERY_STATE_TRACKER,
                    CacheVersion.V1_0
            );

            this.cacheValue = createValue(labelData);
            CacheMap.getCacheMap().add(cacheKey, cacheValue);

            Log.success("Delivery State (Label) registered in scoped cache.");
            return;
        }
        Log.error("Error: Cannot register null shipping label metadata.");
    }

    public void removeDeliveryState() {
        if (this.cacheKey != null && CacheMap.getCacheMap().containsKey(this.cacheKey)) {
            CacheMap.getCacheMap().remove(this.cacheKey);
            this.cacheKey = null;
            this.cacheValue = null;
            Log.success("Delivery State removed from cache.");
        }
    }
}