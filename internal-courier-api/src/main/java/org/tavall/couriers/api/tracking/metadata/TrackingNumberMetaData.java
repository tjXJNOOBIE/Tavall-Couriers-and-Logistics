package org.tavall.couriers.api.tracking.metadata;


import java.util.Objects;
import java.util.UUID;

public record TrackingNumberMetaData(String trackingNumber, UUID qrUuid) {

    /**
     * Compact constructor for validation.
     * If this data is immutable, it better be valid from the jump.
     */
    public TrackingNumberMetaData {
        // Don't let garbage data into your system. Fail fast.
        Objects.requireNonNull(trackingNumber, "Tracking number cannot be null");
        Objects.requireNonNull(qrUuid, "QR UUID cannot be null");

        if (trackingNumber.isBlank()) {
            throw new IllegalArgumentException("Tracking number cannot be empty/blank");
        }
    }
}