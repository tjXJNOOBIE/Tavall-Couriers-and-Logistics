package org.tavall.couriers.api.qr.scan.cache;


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

public class ScanCacheService extends AbstractCache<ScanCacheService,ScanResponse>{

    public static final ScanCacheService INSTANCE = new ScanCacheService();
    private ICacheKey<ScanCacheService> cacheKey;
    ICacheValue<?> cacheValue;
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
        if(scanResponse != null) {
            this.cacheKey = (ICacheKey<ScanCacheService>) createKey(INSTANCE, CacheType.MEMORY, CacheDomain.SCANS, CacheSource.AI_SCANNER, CacheVersion.V1_0);
            this.cacheValue = createValue(scanResponse);
            // Add to a static cache map
            CacheMap.getCacheMap().add(cacheKey,cacheValue);
            Log.success("Scan response registered in cache.");
            return;
        }
        Log.error("Error: Cannot register null scan response.");
    }

    public ICacheKey<ScanCacheService> getScanCacheKey() {
        return this.cacheKey;
    }

    public boolean containsScanKey() {
        if (this.cacheKey != null) {
            return CacheMap.getCacheMap().containsKey(getScanCacheKey());
        }
        return false;
    }





}