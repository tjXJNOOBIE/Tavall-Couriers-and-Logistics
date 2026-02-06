/*
 * TJVD License (TJ Valentine’s Discretionary License) — Version 1.0 (2025)
 *
 * Copyright (c) 2025 Taheesh Valentine
 *
 * This source code is protected under the TJVD License.
 * SEE LICENSE.TXT
 */

package org.tavall.couriers.api.qr.scan.metadata;

import org.tavall.couriers.api.qr.scan.state.ScanIntent;

import java.util.UUID;

public record LocalQRScanData(ScanIntent intent,
                              UUID uuid,
                              String rawContent,
                              boolean isCached) {

    public boolean isValid() {
        return intent != ScanIntent.INVALID_SCAN;
    }
}