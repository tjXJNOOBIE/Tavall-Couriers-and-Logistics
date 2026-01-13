/*
 * TJVD License (TJ Valentine’s Discretionary License) — Version 1.0 (2025)
 *
 * Copyright (c) 2025 Taheesh Valentine
 *
 * This source code is protected under the TJVD License.
 * SEE LICENSE.TXT
 */

package org.tavall.couriers.api.web.endpoints.camera.metadata;

public record ScanResponse(String uuid,
                           String status, // "SEARCHING", "FOUND", "ERROR"
                           String trackingNumber,
                           String name,
                           String address,
                           String phoneNumber,
                           String notes
) {}