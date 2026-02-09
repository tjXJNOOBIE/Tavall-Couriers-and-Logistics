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
        String city,
        String state,
        String zipCode,
        String country,
        String phoneNumber,
        Instant deadline,
        String notes,
        String intakeStatus,
        boolean pendingIntake,
        boolean existingLabel) {

    public ScanResponse {
    }

    public ScanResponse(String uuid,
                        CameraState cameraState,
                        GeminiResponseState geminiResponseState,
                        String trackingNumber,
                        String name,
                        String address,
                        String city,
                        String state,
                        String zipCode,
                        String country,
                        String phoneNumber,
                        Instant deadline,
                        String notes) {
        this(uuid, cameraState, geminiResponseState, trackingNumber, name, address, city, state, zipCode, country,
                phoneNumber, deadline, notes, null, false, false);
    }


    public boolean isMissingCriticalData(ScanResponse raw) {
        return (raw.trackingNumber() == null || raw.trackingNumber().isBlank())
                && (raw.address() == null || raw.address().isBlank());
    }
}
