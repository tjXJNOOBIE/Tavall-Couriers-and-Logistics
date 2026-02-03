/*
 * TJVD License (TJ Valentine’s Discretionary License) — Version 1.0 (2025)
 *
 * Copyright (c) 2025 Taheesh Valentine
 *
 * This source code is protected under the TJVD License.
 * SEE LICENSE.TXT
 */

package org.tavall.couriers.api.cache.interfaces;


import org.tavall.couriers.api.cache.enums.CacheDomain;
import org.tavall.couriers.api.cache.enums.CacheSource;
import org.tavall.couriers.api.cache.enums.CacheType;
import org.tavall.couriers.api.cache.enums.CacheVersion;

public interface ICacheKey<K>{



    default K getRawCacheKey(){
        return null;
    }

    CacheType getCacheType();

    CacheDomain getCacheDomain();

    CacheSource getSource();
    CacheVersion getVersion();

    boolean equals(Object o);
    int hashCode();
    String toString();

}