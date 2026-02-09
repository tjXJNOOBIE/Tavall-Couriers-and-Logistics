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

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

@Component
public class QRShippingLabelCache extends AbstractCache<QRShippingLabelCache, ShippingLabelMetaDataEntity> {
    private ICacheKey<QRShippingLabelCache> cacheKey;
    private ICacheValue<?> cacheValue;
    private final ConcurrentMap<String, ShippingLabelMetaDataEntity> trackingIndex = new ConcurrentHashMap<>();
    private final ConcurrentMap<String, ShippingLabelMetaDataEntity> uuidIndex = new ConcurrentHashMap<>();
    private volatile boolean primed;

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
            ensureCacheKey();
            boolean isNew = indexLabel(labelData);
            if (isNew) {
                this.cacheValue = createValue(labelData);
                // Shove it into the static map
                CacheMap.getCacheMap().add(cacheKey, cacheValue);
            }
            primed = true;

            Log.success("Shipping Label registered in cache.");
            return;
        }
        Log.error("Error: Cannot register null shipping label metadata.");
    }

    public void primeCache(Collection<ShippingLabelMetaDataEntity> labels) {
        if (labels == null) {
            primed = true;
            return;
        }
        ensureCacheKey();
        if (labels.isEmpty()) {
            primed = true;
            return;
        }
        for (ShippingLabelMetaDataEntity label : labels) {
            if (label == null) {
                continue;
            }
            boolean isNew = indexLabel(label);
            if (isNew) {
                this.cacheValue = createValue(label);
                CacheMap.getCacheMap().add(cacheKey, cacheValue);
            }
        }
        primed = true;
        Log.success("Shipping label cache primed with " + labels.size() + " records.");
    }

    public ShippingLabelMetaDataEntity findByTrackingNumber(String trackingNumber) {
        String normalized = normalizeTrackingNumber(trackingNumber);
        if (normalized == null) {
            return null;
        }
        return trackingIndex.get(normalized);
    }

    public List<ShippingLabelMetaDataEntity> findByTrackingNumbers(Collection<String> trackingNumbers) {
        if (trackingNumbers == null || trackingNumbers.isEmpty()) {
            return List.of();
        }
        List<ShippingLabelMetaDataEntity> results = new ArrayList<>();
        for (String trackingNumber : trackingNumbers) {
            ShippingLabelMetaDataEntity label = findByTrackingNumber(trackingNumber);
            if (label != null) {
                results.add(label);
            }
        }
        return results;
    }

    public ShippingLabelMetaDataEntity findByUuid(String uuid) {
        String normalized = normalizeUuid(uuid);
        if (normalized == null) {
            return null;
        }
        return uuidIndex.get(normalized);
    }

    public List<ShippingLabelMetaDataEntity> getAllLabels() {
        if (uuidIndex.isEmpty()) {
            return List.of();
        }
        return new ArrayList<>(uuidIndex.values());
    }

    public boolean isPrimed() {
        return primed;
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

    @SuppressWarnings("unchecked")
    private void ensureCacheKey() {
        if (this.cacheKey == null) {
            this.cacheKey = (ICacheKey<QRShippingLabelCache>) createKey(
                    this,
                    CacheType.MEMORY,
                    CacheDomain.QR,
                    CacheSource.QR_CODE_GENERATOR,
                    CacheVersion.V1_0);
        }
    }

    private boolean indexLabel(ShippingLabelMetaDataEntity labelData) {
        String uuid = normalizeUuid(labelData.getUuid());
        String tracking = normalizeTrackingNumber(labelData.getTrackingNumber());
        ShippingLabelMetaDataEntity previous = null;
        if (uuid != null) {
            previous = uuidIndex.put(uuid, labelData);
        }
        if (previous != null) {
            String previousTracking = normalizeTrackingNumber(previous.getTrackingNumber());
            if (previousTracking != null && !previousTracking.equals(tracking)) {
                trackingIndex.remove(previousTracking, previous);
            }
        }
        if (tracking != null) {
            trackingIndex.put(tracking, labelData);
        }
        return previous == null;
    }

    private String normalizeTrackingNumber(String trackingNumber) {
        if (trackingNumber == null || trackingNumber.isBlank()) {
            return null;
        }
        return trackingNumber.trim().toUpperCase(Locale.ROOT);
    }

    private String normalizeUuid(String uuid) {
        if (uuid == null || uuid.isBlank()) {
            return null;
        }
        return uuid.trim();
    }
}
