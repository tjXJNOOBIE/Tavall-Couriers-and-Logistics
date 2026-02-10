package org.tavall.couriers.api.tracking.cache;


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
import org.tavall.couriers.api.web.entities.tracking.TrackingNumberMetaDataEntity;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

@Component
public class TrackingNumberCache extends AbstractCache<TrackingNumberCache, TrackingNumberMetaDataEntity> {
    private ICacheKey<TrackingNumberCache> cacheKey;
    private ICacheValue<?> cacheValue;
    private final ConcurrentMap<String, TrackingNumberMetaDataEntity> trackingIndex = new ConcurrentHashMap<>();
    private final ConcurrentMap<UUID, TrackingNumberMetaDataEntity> uuidIndex = new ConcurrentHashMap<>();
    private volatile boolean primed;

    /**
     * Constructs a TrackingNumberCache from {@link AbstractCache}.
     */
    public TrackingNumberCache() {
        super();
    }

    @Override
    public CacheType getCacheType() {
        return CacheType.MEMORY;
    }

    @Override
    public CacheDomain getCacheDomain() {
        return CacheDomain.TRACKING;
    }

    @Override
    public CacheSource getSource() {
        return CacheSource.TRACKING_NUMBER_GENERATOR;
    }

    @Override
    public CacheVersion getVersion() {
        return CacheVersion.V1_0;
    }

    @SuppressWarnings("unchecked")
    public void registerTrackingNumber(TrackingNumberMetaDataEntity trackingData) {
        if (trackingData != null) {
            ensureCacheKey();
            boolean isNew = indexTrackingData(trackingData);
            if (isNew) {
                this.cacheValue = createValue(trackingData);
                CacheMap.getCacheMap().add(cacheKey, cacheValue);
            }
            primed = true;

            Log.success("Tracking number registered in cache.");
            return;
        }
        Log.error("Error: Cannot register null tracking number metadata.");
    }

    public void primeCache(Collection<TrackingNumberMetaDataEntity> trackingNumbers) {
        if (trackingNumbers == null) {
            primed = true;
            return;
        }
        ensureCacheKey();
        if (trackingNumbers.isEmpty()) {
            primed = true;
            return;
        }
        for (TrackingNumberMetaDataEntity entity : trackingNumbers) {
            if (entity == null) {
                continue;
            }
            boolean isNew = indexTrackingData(entity);
            if (isNew) {
                this.cacheValue = createValue(entity);
                CacheMap.getCacheMap().add(cacheKey, cacheValue);
            }
        }
        primed = true;
        Log.success("Tracking number cache primed with " + trackingNumbers.size() + " records.");
    }

    public TrackingNumberMetaDataEntity findByTrackingNumber(String trackingNumber) {
        String normalized = normalizeTrackingNumber(trackingNumber);
        if (normalized == null) {
            return null;
        }
        return trackingIndex.get(normalized);
    }

    public List<TrackingNumberMetaDataEntity> findByTrackingNumbers(Collection<String> trackingNumbers) {
        if (trackingNumbers == null || trackingNumbers.isEmpty()) {
            return List.of();
        }
        List<TrackingNumberMetaDataEntity> results = new ArrayList<>();
        for (String trackingNumber : trackingNumbers) {
            TrackingNumberMetaDataEntity entity = findByTrackingNumber(trackingNumber);
            if (entity != null) {
                results.add(entity);
            }
        }
        return results;
    }

    public TrackingNumberMetaDataEntity findByQrUuid(UUID qrUuid) {
        if (qrUuid == null) {
            return null;
        }
        return uuidIndex.get(qrUuid);
    }

    public List<TrackingNumberMetaDataEntity> getAllTrackingNumbers() {
        if (trackingIndex.isEmpty()) {
            return List.of();
        }
        return new ArrayList<>(trackingIndex.values());
    }

    public boolean isPrimed() {
        return primed;
    }
    /**
     * Removes the current tracking number from the global CacheMap and clears local references.
     */
    public void removeTrackingNumber() {
        if (this.cacheKey != null && CacheMap.getCacheMap().containsKey(this.cacheKey)) {
            // Remove from the global static map
            CacheMap.getCacheMap().remove(this.cacheKey);

            // Nullify local state to prevent stale data usage
            this.cacheKey = null;
            this.cacheValue = null;

            Log.success("Tracking number removed from cache.");
        } else {
            Log.error("Error: Tracking number key not found or already removed.");
        }
    }
    public ICacheKey<TrackingNumberCache> getTrackingCacheKey() {
        return this.cacheKey;
    }

    public boolean containsTrackingKey() {
        if (this.cacheKey == null) {
            Log.warn("Tracking cache key missing; cache is empty.");
            return false;
        }
        boolean contains = CacheMap.getCacheMap().containsKey(getTrackingCacheKey());
        Log.info("Tracking cache key present: " + contains);
        return contains;
    }

    @SuppressWarnings("unchecked")
    private void ensureCacheKey() {
        if (this.cacheKey == null) {
            this.cacheKey = (ICacheKey<TrackingNumberCache>) createKey(
                    this,
                    this.getCacheType(),
                    this.getCacheDomain(),
                    this.getSource(),
                    this.getVersion());
        }
    }

    private boolean indexTrackingData(TrackingNumberMetaDataEntity trackingData) {
        String normalized = normalizeTrackingNumber(trackingData.getTrackingNumber());
        TrackingNumberMetaDataEntity previous = null;
        if (normalized != null) {
            previous = trackingIndex.put(normalized, trackingData);
        }
        if (trackingData.getQrUuid() != null) {
            uuidIndex.put(trackingData.getQrUuid(), trackingData);
        }
        return previous == null;
    }

    private String normalizeTrackingNumber(String trackingNumber) {
        if (trackingNumber == null || trackingNumber.isBlank()) {
            return null;
        }
        return trackingNumber.trim().toUpperCase(Locale.ROOT);
    }

}
