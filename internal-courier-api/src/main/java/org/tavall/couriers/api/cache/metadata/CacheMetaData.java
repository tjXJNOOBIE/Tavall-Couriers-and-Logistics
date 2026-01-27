/*
 * TJVD License (TJ Valentine’s Discretionary License) — Version 1.0 (2025)
 *
 * Copyright (c) 2025 TJ Valentine
 *
 * This source code is protected under the TJVD License.
 *
 * No public use, distribution, or modification is permitted without express,
 * written, and verifiable consent from the project founder.
 *
 * Private project files obtained without consent must not be run, opened, or distributed.
 *
 * Public API usage is subject to rate limits for free users.
 *
 * For permissions or inquiries, contact: tj.valentine@example.com
 */

package org.tavall.couriers.api.cache.metadata;


import org.tavall.couriers.api.cache.interfaces.ICacheStats;

import java.util.concurrent.atomic.AtomicInteger;

public class CacheMetaData implements ICacheStats {

    private int totalEntries;
    private int validEntries;
    private final AtomicInteger expiredEntries = new AtomicInteger();

    public CacheMetaData(int totalEntries, int validEntries, int expiredEntries) {
        totalEntries = totalEntries;
        this.validEntries = validEntries;
        this.expiredEntries.set(expiredEntries);
    }

    @Override
    public int getTotalEntries() {
        return totalEntries;
    }
    @Override
    public int getValidEntries() {
        return validEntries;
    }
    @Override
    public int getExpiredEntries() {
        return expiredEntries.get();
    }
    public CacheMetaData addExpiredEntries(int expiredEntries) {
        return new CacheMetaData(totalEntries,validEntries,expiredEntries); // new instance with updated values
    }
    public CacheMetaData addTotalEntries(int totalEntries) {

        return new CacheMetaData(totalEntries,validEntries, expiredEntries.get()); // new instance with updated values
    }
    public CacheMetaData addValidEntries(int validEntries) {
        return new CacheMetaData(totalEntries,validEntries, expiredEntries.get()); // new instance with updated values
    }


    @Override
    public String toString() {
        return String.format("CacheStats{total=%d, valid=%d, expired=%d}",
                totalEntries, validEntries, expiredEntries);
    }
}