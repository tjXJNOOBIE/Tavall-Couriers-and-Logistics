/*
 * TJVD License (TJ Valentine’s Discretionary License) — Version 1.0 (2025)
 *
 * Copyright (c) 2025 Taheesh Valentine
 *
 * This source code is protected under the TJVD License.
 * SEE LICENSE.TXT
 */

package org.tavall.couriers.api.cache.enums;

public enum CacheType {
    MEMORY,        // Fastest, short-lived
    REDIS,         // Shared, distributed, semi-persistent
    DISK,          // Flatfile/SQLite
    HYBRID,        // RAM + fallback
    DATABASE      // Persistent, slowest
}