package org.tavall.springapi.service.cache;


import org.tavall.couriers.api.cache.AbstractCache;
import org.tavall.couriers.api.cache.CacheValue;
import org.tavall.couriers.api.cache.enums.CacheDomain;
import org.tavall.couriers.api.cache.enums.CacheSource;
import org.tavall.couriers.api.cache.enums.CacheType;
import org.tavall.couriers.api.cache.enums.CacheVersion;
import org.tavall.couriers.api.cache.interfaces.ICacheKey;
import org.tavall.couriers.api.cache.interfaces.ICacheValue;
import org.tavall.couriers.api.cache.maps.CacheMap;
import org.tavall.couriers.api.console.Log;
import org.tavall.springapi.scan.metadata.ScanResponse;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class ScanCacheService extends AbstractCache<ScanCacheService,ScanResponse>{


    private CacheMap cacheMap = CacheMap.INSTANCE;
    private ScanCacheService scanCacheService = new ScanCacheService();
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

    public void registerScanResponse(ScanResponse scanResponse) {
        if(scanResponse != null) {
            ICacheKey<?> cacheKey = createKey(scanCacheService, CacheType.MEMORY, CacheDomain.SCANS, CacheSource.AI_SCANNER, CacheVersion.V1_0);
            ICacheValue<?> newValue = createValue(scanResponse);
            cacheMap.add(cacheKey, newValue);
            return;
        }
        Log.error("Error: Cannot register null scan response.");
    }


}