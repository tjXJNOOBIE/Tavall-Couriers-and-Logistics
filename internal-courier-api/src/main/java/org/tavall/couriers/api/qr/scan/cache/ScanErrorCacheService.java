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
import org.tavall.couriers.api.qr.scan.state.CameraState;

import java.util.ArrayList;
import java.util.List;

@Component
public class ScanErrorCacheService extends AbstractCache<ScanErrorCacheService, ScanResponse> {
    private ICacheKey<ScanErrorCacheService> cacheKey;
    private ICacheValue<?> cacheValue;

    @Override
    public CacheType getCacheType() {
        return CacheType.MEMORY;
    }

    @Override
    public CacheDomain getCacheDomain() {
        return CacheDomain.SCAN_ERRORS;
    }

    @Override
    public CacheSource getSource() {
        return CacheSource.SCAN_ERROR_TRACKER;
    }

    @Override
    public CacheVersion getVersion() {
        return CacheVersion.V1_0;
    }

    @SuppressWarnings("unchecked")
    public void registerScanError(ScanResponse scanResponse) {
        if (scanResponse == null) {
            return;
        }
        if (scanResponse.cameraState() != CameraState.ERROR) {
            return;
        }
        ensureCacheKey();
        this.cacheValue = createValue(scanResponse);
        CacheMap.getCacheMap().add(cacheKey, cacheValue);
        Log.warn("Scan error cached: " + (scanResponse.uuid() != null ? scanResponse.uuid() : "unknown"));
    }

    public List<ScanResponse> getRecentErrors(int limit) {
        if (this.cacheKey == null) {
            return List.of();
        }
        List<ICacheValue<?>> bucket = CacheMap.getCacheMap().getBucket(getScanErrorKey());
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

    public int getErrorCount() {
        if (this.cacheKey == null) {
            return 0;
        }
        return CacheMap.getCacheMap().getBucket(getScanErrorKey()).size();
    }

    public String getLatestErrorUuid() {
        List<ScanResponse> errors = getRecentErrors(1);
        if (errors.isEmpty()) {
            return null;
        }
        return errors.get(0).uuid();
    }

    public ICacheKey<ScanErrorCacheService> getScanErrorKey() {
        if (this.cacheKey == null) {
            Log.warn("Scan error cache key requested before initialization.");
        }
        return this.cacheKey;
    }

    @SuppressWarnings("unchecked")
    private void ensureCacheKey() {
        if (this.cacheKey == null) {
            this.cacheKey = (ICacheKey<ScanErrorCacheService>) createKey(
                    this,
                    CacheType.MEMORY,
                    CacheDomain.SCAN_ERRORS,
                    CacheSource.SCAN_ERROR_TRACKER,
                    CacheVersion.V1_0
            );
        }
    }
}
