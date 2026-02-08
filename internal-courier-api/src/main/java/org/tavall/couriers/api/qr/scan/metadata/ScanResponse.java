/*
 * TJVD License (TJ Valentine’s Discretionary License) — Version 1.0 (2025)
 *
 * Copyright (c) 2025 Taheesh Valentine
 *
 * This source code is protected under the TJVD License.
 * SEE LICENSE.TXT
 */

package org.tavall.couriers.api.qr.scan.metadata;


import org.tavall.couriers.api.qr.scan.state.CameraState;
import org.tavall.couriers.api.qr.scan.state.GeminiResponseState;

import java.time.Instant;

public record ScanResponse(String uuid,
        CameraState cameraState,
        GeminiResponseState geminiResponseState,
        String trackingNumber,
        String name,
        String address,
        String city,        // NEW
        String state,       // NEW
        String zipCode,     // NEW
        String country,     // NEW
        String phoneNumber,
        Instant deadline,
        String notes) {


    public boolean isMissingCriticalData(ScanResponse raw) {
        return (raw.trackingNumber() == null || raw.trackingNumber().isBlank())
                && (raw.address() == null || raw.address().isBlank());
    }
}
