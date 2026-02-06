package org.tavall.couriers.api.qr.cache;


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
import org.tavall.couriers.api.qr.metadata.QRMetaData;

import java.util.concurrent.Callable;
import java.util.concurrent.StructuredTaskScope;

public class QRCodeCache extends AbstractCache<QRCodeCache, QRMetaData> {


    public static final ScopedValue<QRCodeCache> CURRENT = ScopedValue.newInstance();

    private ICacheKey<QRCodeCache> cacheKey;
    private ICacheValue<?> cacheValue;

    public QRCodeCache() {
        super();
    }

    // --- STRICT ACCESSOR ---

    public static QRCodeCache get() {
        if (!CURRENT.isBound()) {
            throw new IllegalStateException("CRITICAL: QRCodeCache accessed outside of a 'runAsync' scope. Wrap your execution.");
        }
        return CURRENT.get();
    }

    // --- ASYNC EXECUTION ---

    public static <T> T runAsync(QRMetaData initialData, Callable<T> task)
            throws InterruptedException, StructuredTaskScope.TimeoutException, StructuredTaskScope.FailedException, Exception {

        QRCodeCache cache = new QRCodeCache();

        // Bind scope -> Execute
        return ScopedValue.where(CURRENT, cache).call(() -> {

            // Seed cache if data provided
            if (initialData != null) {
                QRCodeCache.get().registerQRCode(initialData);
            }

            // Delegate to AsyncTask (inherits scope)
            return AsyncTask.runAsync(task);
        });
    }

    // --- CACHE IMPLEMENTATION ---

    @Override
    public CacheType getCacheType() { return CacheType.MEMORY; }

    @Override
    public CacheDomain getCacheDomain() { return CacheDomain.QR; }

    @Override
    public CacheSource getSource() { return CacheSource.QR_CODE_GENERATOR; }

    @Override
    public CacheVersion getVersion() { return CacheVersion.V1_0; }

    @SuppressWarnings("unchecked")
    public void registerQRCode(QRMetaData qrData) {
        if (qrData != null) {
            // Use 'this' because we are inside the instance
            this.cacheKey = (ICacheKey<QRCodeCache>) createKey(
                    this,
                    CacheType.MEMORY,
                    CacheDomain.QR,
                    CacheSource.QR_CODE_GENERATOR,
                    CacheVersion.V1_0
            );

            this.cacheValue = createValue(qrData);
            CacheMap.getCacheMap().add(cacheKey, cacheValue);

            Log.success("QR MetaData registered in scoped cache.");
            return;
        }
        Log.error("Error: Cannot register null QR MetaData.");
    }

    public void removeQRCode() {
        if (this.cacheKey != null && CacheMap.getCacheMap().containsKey(this.cacheKey)) {
            CacheMap.getCacheMap().removeCacheKey(this.cacheKey);
            this.cacheKey = null;
            this.cacheValue = null;
            Log.success("QR MetaData removed from cache.");
        }
    }

    public ICacheKey<QRCodeCache> getQRCodeCacheKey() {
        return this.cacheKey;
    }
}