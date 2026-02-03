/*
 * TJVD License (TJ Valentine’s Discretionary License) — Version 1.0 (2025)
 *
 * Copyright (c) 2025 Taheesh Valentine
 *
 * This source code is protected under the TJVD License.
 * SEE LICENSE.TXT
 */

package org.tavall.couriers.api.cache.interfaces;


import org.checkerframework.checker.units.qual.K;
import org.tavall.couriers.api.cache.enums.CacheType;

public interface ICacheValue<V> {


    default V getValue() {
        return null;
    }




}