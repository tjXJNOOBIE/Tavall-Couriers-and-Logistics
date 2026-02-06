package org.tavall.couriers.api.cache.maps;


import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.tavall.couriers.api.cache.CacheKey;
import org.tavall.couriers.api.cache.CacheValue;
import org.tavall.couriers.api.cache.enums.CacheDomain;
import org.tavall.couriers.api.cache.enums.CacheType;
import org.tavall.couriers.api.cache.interfaces.ICacheKey;
import org.tavall.couriers.api.cache.interfaces.ICacheValue;
import org.tavall.couriers.api.console.Log;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class CacheMapTest {
    private CacheMap cacheMap;

    @BeforeEach
    void setUp() {
        Log.info("--- Setup: Clearing CacheMap Singleton ---");
        cacheMap = CacheMap.getCacheMap();
        cacheMap.clear();
    }

    @Test
    void testAdd_NewKey_ShouldCreateList() {
        Log.info("Starting test: testAdd_NewKey_ShouldCreateList");

        ICacheKey<String> key = new CacheKey<>("new-key", CacheType.MEMORY, CacheDomain.SCANS);
        ICacheValue<String> value = new CacheValue<>("data", System.currentTimeMillis() + 5000);

        Log.info("Attempting to ADD new key: " + key);

        // CALLING YOUR BROKEN ADD METHOD
        cacheMap.add(key, value);

        if (!cacheMap.containsKey(key)) {
            Log.error("Map does NOT contain key after add(). The add() method logic failed to initialize the list.");
        } else {
            Log.success("Map successfully added new key.");
        }

        assertTrue(cacheMap.containsKey(key), "Map should contain key after adding");
        assertEquals(1, cacheMap.get(key).size());

        Log.success("New Key addition verified.");
    }

    @Test
    void testAdd_ExistingKey_ShouldAppend() {
        Log.info("Starting test: testAdd_ExistingKey_ShouldAppend");

        // 1. Setup
        ICacheKey<String> key = new CacheKey<>("existing-key", CacheType.MEMORY);
        ICacheValue<String> val1 = new CacheValue<>("v1", 10000);
        ICacheValue<String> val2 = new CacheValue<>("v2", 10000);

        // 2. Execution
        Log.info("First value added..");
        cacheMap.add(key, val1);

        Log.info("Calling add() for second value...");
        cacheMap.add(key, val2);

        // 3. Retrieval
        // We get the list associated with the key
        List<ICacheValue<?>> bucket = cacheMap.get(key);

        // 4. Verification
        // Assertion A: The Map should only have 1 Key entry
        assertEquals(1, cacheMap.size(), "Map should contain exactly 1 Key entry");

        // Assertion B: The Bucket (List) should exist
        assertNotNull(bucket, "The value bucket should not be null");

        // Assertion C: The Bucket should contain exactly 2 items
        Log.info("Bucket size: " + bucket.size());
        assertEquals(2, bucket.size(), "The bucket should contain both added values");

        // Assertion D: The content matches (Optional but good)
        assertTrue(bucket.contains(val1), "Bucket should contain val1");
        assertTrue(bucket.contains(val2), "Bucket should contain val2");

        Log.success("Existing Key append verified.");
    }

    @Test
    void testFindByDomain() {
        Log.info("Starting test: testFindByDomain");

        CacheKey<String> k1 = new CacheKey<>("d1", CacheType.MEMORY, CacheDomain.SCANS);
        CacheKey<String> k2 = new CacheKey<>("d2", CacheType.MEMORY, CacheDomain.QR);

        cacheMap.put(k1, new java.util.ArrayList<>());
        cacheMap.put(k2, new java.util.ArrayList<>());

        List<List<ICacheValue<?>>> scanResults = cacheMap.findByDomain(CacheDomain.SCANS);
        Log.info("Found " + scanResults.size() + " lists for Domain SCANS");
        assertEquals(1, scanResults.size());

        List<List<ICacheValue<?>>> qrResults = cacheMap.findByDomain(CacheDomain.QR);
        Log.info("Found " + qrResults.size() + " lists for Domain QR");
        assertEquals(1, qrResults.size());

        Log.success("Domain lookup verified.");
    }
}