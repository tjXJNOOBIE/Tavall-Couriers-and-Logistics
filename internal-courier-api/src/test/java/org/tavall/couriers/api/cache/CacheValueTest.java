package org.tavall.couriers.api.cache;


import org.junit.jupiter.api.Test;
import org.tavall.couriers.api.cache.interfaces.ICacheValue;
import org.tavall.couriers.api.console.Log;
import org.tavall.couriers.api.utils.scheduler.CustomRunnable;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.*;

public class CacheValueTest {

    @Test
    void testValueRetrieval() {
        Log.info("Starting test: testValueRetrieval");

        String data = "Payload";
        long expiry = System.currentTimeMillis() + 10000;
        ICacheValue<String> cacheValue = new CacheValue<>(data, expiry);
        ICacheValue<String> sameValue = new CacheValue<>(data, expiry + 500);
        Log.info("Comparing two CacheValues with same payload but different expiry...");
        Log.warn("Value: "+cacheValue.getValue() );

        Log.warn("Same Value: "+sameValue.getValue() );

        assertEquals(cacheValue, sameValue, "CacheValues should be equal if underlying values are equal");
        Log.success("Value equality check passed.");
    }
    @Test
    void testExpiration() {
        Log.info("Starting test: testExpiration");

        long past = System.currentTimeMillis() - 1000;
        long future = System.currentTimeMillis() + 10000;

        CacheValue<String> expiredVal = new CacheValue<>("Old", past);
        CacheValue<String> freshVal = new CacheValue<>("New", future);

        Log.info("Checking expired value...");
        if (expiredVal.isExpired()) {
            Log.success("Expired value correctly identified.");
        } else {
            Log.error("Expired value failed check!");
        }
        assertTrue(expiredVal.isExpired(), "Should be expired");

        Log.info("Checking fresh value...");
        if (!freshVal.isExpired()) {
            Log.success("Fresh value correctly identified.");
        } else {
            Log.error("Fresh value marked as expired!");
        }
        assertFalse(freshVal.isExpired(), "Should be valid");

        Log.success("Expiration logic verified.");
    }
    @Test
    void testExpirationWithRunnable() throws InterruptedException {
        Log.info("Starting test: testExpiration (Async via CustomRunnable)");

        long ttlMs = 100;
        long safetyMargin = 150;
        CacheValue<String> shortLivedVal = new CacheValue<>("Transient", System.currentTimeMillis() + ttlMs);

        CountDownLatch latch = new CountDownLatch(1);
        AtomicBoolean expirationDetected = new AtomicBoolean(false);

        // Define the CustomRunnable
        CustomRunnable checkTask = new CustomRunnable() {
            @Override
            public void run() {
                Log.warn("Value: "+shortLivedVal.getValue());
                try {
                    Log.info("CustomRunnable: Checking expiration status...");
                    if (shortLivedVal.isExpired()) {
                        expirationDetected.set(true);
                        Log.success("CustomRunnable: Value is expired as expected.");
                        Log.warn("Value: "+shortLivedVal.getValue());

                    } else {
                        Log.error("CustomRunnable: Value should be expired but isn't!");
                    }
                } finally {
                    latch.countDown();
                }
            }
        };

        try {
            Log.info("Scheduling check task...");
            checkTask.runTaskLater(ttlMs + safetyMargin);

            boolean finishedInTime = latch.await(2, TimeUnit.SECONDS);

            if (!finishedInTime) {
                Log.critical("Test timed out! CustomRunnable never fired.");
            }
            assertTrue(finishedInTime, "Async task timed out.");
            assertTrue(expirationDetected.get(), "The cache value did not expire when checked asynchronously.");

            Log.success("Async expiration logic verified.");

        } finally {
            checkTask.shutdown();
            Log.info("Teardown: CustomRunnable scheduler shutdown.");
        }
    }
}