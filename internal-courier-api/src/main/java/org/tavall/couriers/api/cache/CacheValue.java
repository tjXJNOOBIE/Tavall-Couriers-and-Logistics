/*
 * TJVD License (TJ Valentine’s Discretionary License) — Version 1.0 (2025)
 *
 * Copyright (c) 2025 Taheesh Valentine
 *
 * This source code is protected under the TJVD License.
 * SEE LICENSE.TXT
 */

package org.tavall.couriers.api.cache;


import org.tavall.couriers.api.cache.enums.CacheType;
import org.tavall.couriers.api.cache.interfaces.ICacheValue;

import java.util.Objects;

public class CacheValue<V> implements ICacheValue<V> {

    private final V value;
    private final long expirationTime;

    public CacheValue(V value, long expirationTime) {
        this.value = value;
        this.expirationTime = expirationTime;
    }


    public boolean isExpired() {
        return isValueExpired(System.currentTimeMillis());
    }

    public boolean isValueExpired(long currentTime) {
        return currentTime > expirationTime;
    }
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;

        // Check if it's a wrapper
        if (o instanceof CacheValue) {
            CacheValue<?> that = (CacheValue<?>) o;
            return Objects.equals(this.value, that.value);
        }

        return false;
    }
    @Override
    public int hashCode() {
        return Objects.hash(value);
    }
    @Override
    public String toString() {
        return "CacheValue{" +
                "value=" + value +
                ", expirationTime=" + expirationTime +
                '}';
    }
}