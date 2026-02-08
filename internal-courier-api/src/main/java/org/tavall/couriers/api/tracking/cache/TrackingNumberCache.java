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

@Component
public class TrackingNumberCache extends AbstractCache<TrackingNumberCache, TrackingNumberMetaDataEntity> {
    private ICacheKey<TrackingNumberCache> cacheKey;
    private ICacheValue<?> cacheValue;

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
            // Create the key specifically for this tracking instance
            this.cacheKey = (ICacheKey<TrackingNumberCache>) createKey(
                    this,
                    this.getCacheType(),
                    this.getCacheDomain(),
                    this.getSource(),
                    this.getVersion());

            this.cacheValue = createValue(trackingData);

            // Push to the global static cache map
            CacheMap.getCacheMap().add(cacheKey, cacheValue);

            Log.success("Tracking number registered in cache.");
            return;
        }
        Log.error("Error: Cannot register null tracking number metadata.");
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

}
