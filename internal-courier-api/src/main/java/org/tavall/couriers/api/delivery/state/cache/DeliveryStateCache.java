package org.tavall.couriers.api.delivery.state.cache;


import org.springframework.stereotype.Component;
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
import org.tavall.couriers.api.web.entities.ShippingLabelMetaDataEntity;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.StructuredTaskScope;
@Component
public class DeliveryStateCache extends AbstractCache<DeliveryStateCache, ShippingLabelMetaDataEntity> {

    // If you don't bind it, it crashes. This is a feature, not a bug.
    public static final ScopedValue<DeliveryStateCache> CURRENT = ScopedValue.newInstance();
    private ICacheKey<DeliveryStateCache> cacheKey;
    private ICacheValue<?> cacheValue;
    private final ConcurrentMap<String, ShippingLabelMetaDataEntity> trackingIndex = new ConcurrentHashMap<>();
    private final ConcurrentMap<String, ShippingLabelMetaDataEntity> uuidIndex = new ConcurrentHashMap<>();
    private volatile boolean primed;

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

    public static <T> T runAsync(ShippingLabelMetaDataEntity initialData, Callable<T> task)
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
    public void registerDeliveryState(ShippingLabelMetaDataEntity labelData) {
        if (labelData != null) {
            // Create key referencing 'this' instance
            ensureCacheKey();
            boolean isNew = indexLabel(labelData);
            if (isNew) {
                this.cacheValue = createValue(labelData);
                CacheMap.getCacheMap().add(cacheKey, cacheValue);
            }
            primed = true;

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

    public void primeCache(Collection<ShippingLabelMetaDataEntity> labels) {
        if (labels == null) {
            primed = true;
            return;
        }
        ensureCacheKey();
        if (labels.isEmpty()) {
            primed = true;
            return;
        }
        for (ShippingLabelMetaDataEntity label : labels) {
            if (label == null) {
                continue;
            }
            boolean isNew = indexLabel(label);
            if (isNew) {
                this.cacheValue = createValue(label);
                CacheMap.getCacheMap().add(cacheKey, cacheValue);
            }
        }
        primed = true;
        Log.success("Delivery state cache primed with " + labels.size() + " records.");
    }

    public ShippingLabelMetaDataEntity findByTrackingNumber(String trackingNumber) {
        String normalized = normalizeTrackingNumber(trackingNumber);
        if (normalized == null) {
            return null;
        }
        return trackingIndex.get(normalized);
    }

    public ShippingLabelMetaDataEntity findByUuid(String uuid) {
        String normalized = normalizeUuid(uuid);
        if (normalized == null) {
            return null;
        }
        return uuidIndex.get(normalized);
    }

    public List<ShippingLabelMetaDataEntity> getAllLabels() {
        if (uuidIndex.isEmpty()) {
            return List.of();
        }
        return new ArrayList<>(uuidIndex.values());
    }

    public boolean isPrimed() {
        return primed;
    }

    @SuppressWarnings("unchecked")
    private void ensureCacheKey() {
        if (this.cacheKey == null) {
            this.cacheKey = (ICacheKey<DeliveryStateCache>) createKey(
                    this,
                    CacheType.MEMORY,
                    CacheDomain.DELIVERY,
                    CacheSource.DELIVERY_STATE_TRACKER,
                    CacheVersion.V1_0
            );
        }
    }

    private boolean indexLabel(ShippingLabelMetaDataEntity labelData) {
        String uuid = normalizeUuid(labelData.getUuid());
        String tracking = normalizeTrackingNumber(labelData.getTrackingNumber());
        ShippingLabelMetaDataEntity previous = null;
        if (uuid != null) {
            previous = uuidIndex.put(uuid, labelData);
        }
        if (previous != null) {
            String previousTracking = normalizeTrackingNumber(previous.getTrackingNumber());
            if (previousTracking != null && !previousTracking.equals(tracking)) {
                trackingIndex.remove(previousTracking, previous);
            }
        }
        if (tracking != null) {
            trackingIndex.put(tracking, labelData);
        }
        return previous == null;
    }

    private String normalizeTrackingNumber(String trackingNumber) {
        if (trackingNumber == null || trackingNumber.isBlank()) {
            return null;
        }
        return trackingNumber.trim().toUpperCase(Locale.ROOT);
    }

    private String normalizeUuid(String uuid) {
        if (uuid == null || uuid.isBlank()) {
            return null;
        }
        return uuid.trim();
    }
}
