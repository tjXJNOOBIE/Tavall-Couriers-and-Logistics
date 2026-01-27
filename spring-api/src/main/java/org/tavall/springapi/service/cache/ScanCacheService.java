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

    @SuppressWarnings("unchecked")
    public void registerScanResponse(ScanResponse scanResponse) {
        // 1. Build the Key and Value
        ICacheKey<?> cacheKey = createKey(scanCacheService,CacheType.MEMORY, CacheDomain.SCANS, CacheSource.AI_SCANNER, CacheVersion.V1_0);
        ICacheValue<?> newValue = createValue(scanResponse);

        // 2. Compute: Handles "Get List OR Create List" + "Add" in one atomic step
        CacheMap.INSTANCE.compute(cacheKey, (k, newMapValue) -> {

            List<ICacheValue<?>> valueList;
            // If null, make new List. If exists, cast the existing value to List.
            if(newMapValue != null) {
                valueList = newMapValue;
                valueList.add(newValue);

                return valueList;
            }
            Log.error("Error: ScanResponse not found in cache. Key: " + cacheKey);


            // Returns the List (which map stores as the Value)
            return null; // Assuming Map allows raw List or you wrap this List in a CacheValue
        });
    }


}