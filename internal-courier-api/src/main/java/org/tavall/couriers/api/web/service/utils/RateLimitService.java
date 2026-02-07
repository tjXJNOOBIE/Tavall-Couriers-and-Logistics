package org.tavall.couriers.api.web.service.utils;

import org.springframework.stereotype.Service;
import org.tavall.couriers.api.utils.scheduler.CustomRunnable;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

@Service
public class RateLimitService {

    // Map of IP Address -> Last Request Epoch Milli
    private final ConcurrentHashMap<String, Long> clientAccessMap = new ConcurrentHashMap<>();
    private int DEFAULT_COOLDOWN_MS = 4000;
    /**
     * Checks if a client is allowed to proceed based on a cooldown.
     * @param clientIp The IP address of the caller.
     * @param cooldownMs The required wait time in milliseconds.
     * @return true if they can proceed, false if they're spamming.
     */
    public boolean isAllowed(String clientIp, int cooldownMs) {
        long currentTime = System.currentTimeMillis();

        // Compute the decision atomically. We don't want race conditions
        // allowing two requests at the same exact millisecond.
        Long lastAccess = clientAccessMap.putIfAbsent(clientIp, currentTime);

        if (lastAccess == null) {
            // First time seeing this IP, they're good to go.
            return true;
        }

        if (currentTime - lastAccess < cooldownMs) {
            // They're clicking too fast. Slap those hands.
            return false;
        }

        // Update the timestamp for the next check and allow.
        clientAccessMap.put(clientIp, currentTime);
        return true;
    }


    public void clearOldRecords() {
        new CustomRunnable() {
            @Override
            public void run() {
            clientAccessMap.clear();
            }
        }.runTaskTimerAsync(DEFAULT_COOLDOWN_MS, TimeUnit.MINUTES.toMillis(5));
    }
}