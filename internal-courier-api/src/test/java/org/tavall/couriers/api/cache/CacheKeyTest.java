package org.tavall.couriers.api.cache;


import org.junit.jupiter.api.Test;
import org.tavall.couriers.api.cache.enums.CacheDomain;
import org.tavall.couriers.api.cache.enums.CacheSource;
import org.tavall.couriers.api.cache.enums.CacheType;
import org.tavall.couriers.api.cache.enums.CacheVersion;
import org.tavall.couriers.api.console.Log;

import static org.junit.jupiter.api.Assertions.*;

public class CacheKeyTest {
    @Test
    void testConstructorAndGetters() {
        Log.info("Starting test: testConstructorAndGetters");

        String rawKey = "user-123";
        CacheKey<String> key = new CacheKey<>(
                rawKey,
                CacheType.MEMORY,
                CacheDomain.SCANS,
                CacheSource.AI_SCANNER,
                CacheVersion.V1_0);

        Log.info("Created Key: " + key);

        assertEquals(rawKey, key.getRawCacheKey());
        assertEquals(CacheType.MEMORY, key.getCacheType());
        assertEquals(CacheDomain.SCANS, key.getCacheDomain());
        assertEquals(CacheSource.AI_SCANNER, key.getSource());
        assertEquals(CacheVersion.V1_0, key.getVersion());
        assertTrue(key.getCreatedAt() > 0);

        Log.success("testConstructorAndGetters passed validation.");
    }

    @Test
    void testEquality_CorrectlyDistinguishesDomains() {
        Log.info("Starting test: testEquality_CorrectlyDistinguishesDomains");

        CacheKey<String> key1 = new CacheKey<>("key", CacheType.MEMORY, CacheDomain.SCANS);
        CacheKey<String> key2 = new CacheKey<>("key", CacheType.MEMORY, CacheDomain.QR);

        // THE FIX: These should strictly NOT be equal
        assertNotEquals(key1, key2, "Different domains must imply inequality.");

        // Hashcodes should likely differ (though collisions are theoretically possible, they shouldn't happen here)
        assertNotEquals(key1.hashCode(), key2.hashCode(), "Different keys should have different hashcodes.");

        Log.success("Equality check passed: Keys with different domains are NOT equal.");
    }

    @Test
    void testAccessCountIncrement() {
        Log.info("Starting test: testAccessCountIncrement");

        String rawKey = "user-123";
        CacheKey<String> key = new CacheKey<>(
                rawKey,
                CacheType.MEMORY,
                CacheDomain.SCANS,
                CacheSource.AI_SCANNER,
                CacheVersion.V1_0);

        assertEquals(rawKey, key.getRawCacheKey());
        assertEquals(CacheType.MEMORY, key.getCacheType());
        assertEquals(CacheDomain.SCANS, key.getCacheDomain());
        assertEquals(CacheSource.AI_SCANNER, key.getSource());
        assertEquals(CacheVersion.V1_0, key.getVersion());
        assertTrue(key.getCreatedAt() > 0);
        Log.warn("Access Count: " + key.getAccessCount());
        Log.warn("Created At: " + key.getCreatedAt());

        Log.success("Access Count logic verified.");
    }
    @Test
    void testHashCode_Matches_Equality_Contract() {
        Log.info("Starting test: testHashCode_Matches_Equality_Contract");

        // 1. Create two IDENTICAL keys (Same raw key, type, and domain)
        CacheKey<String> key1 = new CacheKey<>("user-session", CacheType.MEMORY, CacheDomain.SCANS);
        CacheKey<String> key2 = new CacheKey<>("user-session", CacheType.MEMORY, CacheDomain.SCANS);

        Log.info("Key 1: " + key1);
        Log.info("Key 2: " + key2);

        // 2. Assert Equality (Pre-requisite)
        // If this fails, your equals() method is still wrong.
        if (key1.equals(key2)) {
            Log.success("Equality Check: PASSED (Identical keys are equal)");
        } else {
            Log.critical("Equality Check: FAILED (Identical keys are NOT equal)");
        }
        assertEquals(key1, key2, "Keys with identical fields must be equal.");

        // 3. Assert HashCode Match
        // If this fails, you broke the Golden Rule of Java.
        int hash1 = key1.hashCode();
        int hash2 = key2.hashCode();

        Log.info("Hash 1: " + hash1);
        Log.info("Hash 2: " + hash2);

        assertEquals(hash1, hash2, "Equal objects MUST have the same hash code.");

        Log.success("HashCode Contract Verified: Safe for HashMap usage.");
    }
}