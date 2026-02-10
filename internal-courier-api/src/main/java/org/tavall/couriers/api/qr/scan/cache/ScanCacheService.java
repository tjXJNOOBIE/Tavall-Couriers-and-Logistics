package org.tavall.couriers.api.qr.scan.cache;


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
import org.tavall.couriers.api.qr.scan.metadata.ScanResponse;

import java.util.ArrayList;
import java.util.List;

@Component
public class ScanCacheService extends AbstractCache<ScanCacheService, ScanResponse> {
    private ICacheKey<ScanCacheService> cacheKey;
    private ICacheValue<?> cacheValue;
    /**
     * Constructs a ScanCacheService from {@link AbstractCache} with a 5-minute default TTL.
     */
    public ScanCacheService() {
        super();
    }


    @Override
    public CacheType getCacheType() {

        return CacheType.MEMORY;
    }


    @Override
    public CacheDomain getCacheDomain() {

        return CacheDomain.SCANS;
    }


    @Override
    public CacheSource getSource() {

        return CacheSource.AI_SCANNER;
    }


    @Override
    public CacheVersion getVersion() {
        return CacheVersion.V1_0;
    }

    @SuppressWarnings("unchecked")
    public void registerScanResponse(ScanResponse scanResponse) {
        if (scanResponse != null) {
            Log.info("Caching scan response: state=" + scanResponse.cameraState()
                    + ", uuid=" + scanResponse.uuid()
                    + ", tracking=" + scanResponse.trackingNumber());
            if (scanResponse.uuid() == null || scanResponse.uuid().isBlank()) {
                Log.warn("Scan response has no UUID; cache entry will be stored without a UUID.");
            }
            this.cacheKey = (ICacheKey<ScanCacheService>) createKey(
                    this,
                    CacheType.MEMORY,
                    CacheDomain.SCANS,
                    CacheSource.AI_SCANNER,
                    CacheVersion.V1_0
            );
            this.cacheValue = createValue(scanResponse);
            // Add to a static cache map
            CacheMap.getCacheMap().add(cacheKey, cacheValue);
            Log.success("Scan response registered in cache.");
            return;
        }
        Log.error("Error: Cannot register null scan response.");
    }

    public List<ScanResponse> getRecentResponses(int limit) {
        if (this.cacheKey == null) {
            return List.of();
        }
        List<ICacheValue<?>> bucket = CacheMap.getCacheMap().getBucket(getScanCacheKey());
        if (bucket.isEmpty()) {
            return List.of();
        }
        List<ScanResponse> responses = new ArrayList<>();
        for (int i = bucket.size() - 1; i >= 0; i--) {
            ScanResponse response = CacheMap.getCacheMap().unwrap(bucket.get(i), ScanResponse.class);
            if (response != null) {
                responses.add(response);
            }
            if (limit > 0 && responses.size() >= limit) {
                break;
            }
        }
        return responses;
    }

    public ICacheKey<ScanCacheService> getScanCacheKey() {
        if (this.cacheKey == null) {
            Log.warn("Scan cache key requested before initialization.");
        }
        return this.cacheKey;
    }

    public boolean containsScanKey() {
        if (this.cacheKey == null) {
            Log.warn("Scan cache key missing; cache is empty.");
            return false;
        }
        boolean contains = CacheMap.getCacheMap().containsKey(getScanCacheKey());
        Log.info("Scan cache key present: " + contains);
        return contains;
    }





}
