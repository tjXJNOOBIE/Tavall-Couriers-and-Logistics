package org.tavall.couriers.api.qr.cache;


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
import org.tavall.couriers.api.web.entities.ShippingLabelMetaDataEntity;

@Component
public class QRShippingLabelCache extends AbstractCache<QRShippingLabelCache, ShippingLabelMetaDataEntity> {
    private ICacheKey<QRShippingLabelCache> cacheKey;
    private ICacheValue<?> cacheValue;

    @Override
    public CacheType getCacheType() {
        return CacheType.MEMORY;
    }
    @Override
    public CacheDomain getCacheDomain() {

        return CacheDomain.QR;
    }

    @Override
    public CacheSource getSource() {

        return CacheSource.QR_CODE_GENERATOR;
    }

    @Override
    public CacheVersion getVersion() {

        return CacheVersion.V1_0;
    }

    @SuppressWarnings("unchecked")
    public void registerShippingLabel(ShippingLabelMetaDataEntity labelData) {
        if (labelData != null) {
            // Create the key for the QR domain
            this.cacheKey = (ICacheKey<QRShippingLabelCache>) createKey(
                    this,
                    CacheType.MEMORY,
                    CacheDomain.QR,
                    CacheSource.QR_CODE_GENERATOR,
                    CacheVersion.V1_0);

            this.cacheValue = createValue(labelData);

            // Shove it into the static map
            CacheMap.getCacheMap().add(cacheKey, cacheValue);

            Log.success("Shipping Label registered in cache.");
            return;
        }
        Log.error("Error: Cannot register null shipping label metadata.");
    }
    /**
     * Removes the current shipping label from the global CacheMap and clears local references.
     */
    public void removeShippingLabel() {
        if (this.cacheKey != null && CacheMap.getCacheMap().containsKey(this.cacheKey)) {
            // Remove from the global static map
            CacheMap.getCacheMap().remove(this.cacheKey);

            // Nullify local state
            this.cacheKey = null;
            this.cacheValue = null;

            Log.success("Shipping Label removed from cache.");
        } else {
            Log.error("Error: Shipping Label key not found or already removed.");
        }
    }
    public ICacheKey<QRShippingLabelCache> getLabelCacheKey() {
        return this.cacheKey;
    }

    public boolean containsLabelKey() {
        if (this.cacheKey == null) {
            Log.warn("Shipping label cache key missing; cache is empty.");
            return false;
        }
        boolean contains = CacheMap.getCacheMap().containsKey(getLabelCacheKey());
        Log.info("Shipping label cache key present: " + contains);
        return contains;
    }
}
