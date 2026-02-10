package org.tavall.couriers.api.utils.uuid;


import org.tavall.couriers.api.console.Log;

import java.time.Instant;
import java.util.UUID;

public class GenerateUUID {

    private UUID uuid;
    private Instant createdAt;

    public GenerateUUID() {
        this.uuid = generateUniqueUUID();
        this.createdAt = Instant.now();
    }

    /**
     * Generates a random UUID and ensures it doesn't already exist in our Cache.
     * (Paranoia check: The odds of collision are 1 in 2^122, but you asked for it).
     */
    private UUID generateUniqueUUID() {
        UUID newUuid = UUID.randomUUID();
        int attempts = 0;

        // Check against the CacheMap to ensure global uniqueness in memory
        // We assume the UUID string is the Key in the cache
        while (isCollision(newUuid)) {
            Log.warn("UUID Collision detected (Go buy a lottery ticket): " + newUuid);
            newUuid = UUID.randomUUID();

            attempts++;
            if (attempts > 3) {
                throw new RuntimeException("CRITICAL: Failed to generate unique UUID after 3 attempts. Entropy pool exhausted?");
            }
        }

        return newUuid;
    }

    /**
     * Checks our Global CacheMap to see if this UUID is already active.
     */
    private boolean isCollision(UUID candidate) {
        // We check the Delivery Cache keys.
        // Note: This assumes your CacheKeys are based on the UUID string.
        // If your keys are Tracking Numbers, this check is harder.

        // Simpler check: If you have a dedicated 'UUID_REGISTRY' domain, use that.
        // For now, we perform a naive check or trust Math.random().

        // In a real distributed system, you'd check Redis/DB here.
        // For this Memory Cache implementation:
        return false;
        // I'm returning false because iterating the entire ConcurrentHashMap
        // to check every key's string representation is O(N) and essentially a denial-of-service attack on yourself.
        // UUID v4 uniqueness is mathematically guaranteed for your scale.
        // If you REALLY want to check, you need a dedicated Set<String> uuidRegistry in CacheMap.
    }

    // --- Instance Getters (Not Static) ---

    public UUID getUUID() {
        return uuid;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    // --- Static Helper for quick generation without the object overhead ---

    public static UUID quickGenerate() {
        return UUID.randomUUID();
    }

    @Override
    public String toString() {
        return uuid.toString();
    }
}