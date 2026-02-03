/*
 * TJVD License (TJ Valentine’s Discretionary License) — Version 1.0 (2025)
 *
 * Copyright (c) 2025 Taheesh Valentine
 *
 * This source code is protected under the TJVD License.
 * SEE LICENSE.TXT
 */

package org.tavall.couriers.api.cache;


import org.tavall.couriers.api.cache.enums.CacheDomain;
import org.tavall.couriers.api.cache.enums.CacheSource;
import org.tavall.couriers.api.cache.enums.CacheType;
import org.tavall.couriers.api.cache.enums.CacheVersion;
import org.tavall.couriers.api.cache.interfaces.ICacheKey;

import java.util.Objects;
import java.util.concurrent.atomic.AtomicInteger;

public class CacheKey<K> implements ICacheKey<K> {

    private K rawKey;
    private CacheType cacheType;
    private CacheDomain cacheDomain;
    private CacheSource source; //changed
    private CacheVersion version;
    private long createdAt;
    private int hashCode;
    private AtomicInteger accessCount = new AtomicInteger(0);


    public CacheKey(K rawKey, CacheType cacheType, CacheDomain cacheDomain, CacheSource source, CacheVersion version) {
        this.rawKey = rawKey;
        this.cacheType = cacheType;
        this.cacheDomain = cacheDomain;
        this.source = source;
        this.version = version;
        this.createdAt = System.currentTimeMillis();
        this.hashCode = computeHashCode();
        this.accessCount.incrementAndGet();
    }

    // --- CHAINED CONSTRUCTORS (Propagating Raw Key) ---
    public CacheKey(K rawKey) {
        this(rawKey, null, null, null, null);
    }
    public CacheKey(K rawKey, CacheType cacheType) {
        this(rawKey, cacheType, null, null, null);
    }

    public CacheKey(K rawKey, CacheType cacheType, CacheDomain domain) {
        this(rawKey, cacheType, domain, null, null);
    }

    public CacheKey(K rawKey, CacheType cacheType, CacheSource source) {
        this(rawKey, cacheType, null, source, null);
    }

    public CacheKey(K rawKey, CacheType cacheType, CacheVersion version) {
        this(rawKey, cacheType, null, null, version);
    }

    public CacheKey(K rawKey, CacheType cacheType, CacheDomain domain, CacheSource source) {
        this(rawKey, cacheType, domain, source, null);
    }

    public CacheKey(K rawKey, CacheType cacheType, CacheSource source, CacheVersion version) {
        this(rawKey, cacheType, null, source, version);
    }

    public CacheKey(K rawKey, CacheType cacheType, CacheDomain domain, CacheVersion version) {
        this(rawKey, cacheType, domain, null, version);
    }
    @Override
    public K getRawCacheKey() {
        return rawKey;
    }

    @Override
    public CacheType getCacheType() {

        return cacheType;
    }


    @Override
    public CacheDomain getCacheDomain() {

        return cacheDomain;
    }


    @Override
    public CacheSource getSource() {

        return source;
    }


    @Override
    public CacheVersion getVersion() {

        return version;
    }


    private int computeHashCode() {

        return Objects.hash(getRawCacheKey(), cacheType, cacheDomain, version, source);
    }


    public int incrementAccessCount() {

        return accessCount.incrementAndGet();
    }


    public int getAccessCount() {

        return accessCount.get();
    }


    public long getCreatedAt() {

        return createdAt;
    }


    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof CacheKey)) return false;
        CacheKey<?> that = (CacheKey<?>) o;

        return Objects.equals(rawKey, that.rawKey) &&
                cacheType == that.getCacheType() &&
                cacheDomain == that.getCacheDomain() &&
                version == that.getVersion() &&
                source == that.getSource();
    }


    @Override
    public int hashCode() {

        return hashCode;
    }


    @Override
    public String toString() {

        return "UserCacheKey{" +
                "key=" + rawKey +
                ", cacheType=" + cacheType +
                ", cacheDomain=" + cacheDomain +
                ", version=" + version +
                ", source=" + source +
                ", accessCount=" + accessCount +
                ", createdAt=" + createdAt +
                '}';
    }
}