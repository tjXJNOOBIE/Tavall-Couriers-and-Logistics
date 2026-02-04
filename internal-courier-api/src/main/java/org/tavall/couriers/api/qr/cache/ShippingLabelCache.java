package org.tavall.couriers.api.qr.cache;


import org.tavall.couriers.api.cache.AbstractCache;
import org.tavall.couriers.api.cache.enums.CacheDomain;
import org.tavall.couriers.api.cache.enums.CacheSource;
import org.tavall.couriers.api.cache.enums.CacheType;
import org.tavall.couriers.api.cache.enums.CacheVersion;
import org.tavall.couriers.api.shipping.ShippingLabelMetaData;

public class ShippingLabelCache extends AbstractCache<ShippingLabelCache, ShippingLabelMetaData> {

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

}