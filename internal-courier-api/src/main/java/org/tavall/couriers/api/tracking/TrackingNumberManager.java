package org.tavall.couriers.api.tracking;


import org.tavall.couriers.api.tracking.metadata.TrackingNumberMetaData;

import java.security.SecureRandom;
import java.util.UUID;

public class TrackingNumberManager {

    private SecureRandom RANDOM = new SecureRandom();
    private String PREFIX = "TAVALL-";
    private static final int DIGIT_COUNT = 14; // 14 Digits = 100 trillion combos


    public TrackingNumberMetaData createTrackingNumber(UUID qrUuid) {
        if (qrUuid == null) {
            throw new IllegalArgumentException("UUID is null. C'mon man, give me something to work with.");
        }

        String trackingNumber = generateTrackingNumber();

        return new TrackingNumberMetaData(trackingNumber, qrUuid);
    }

    /**
     * Generates "TAVALL-" followed by 14 random digits.
     */
    private String generateTrackingNumber() {
        StringBuilder sb = new StringBuilder(PREFIX.length() + DIGIT_COUNT);
        sb.append(PREFIX);

        for (int i = 0; i < DIGIT_COUNT; i++) {
            sb.append(RANDOM.nextInt(10));
        }
        return sb.toString();
    }
}