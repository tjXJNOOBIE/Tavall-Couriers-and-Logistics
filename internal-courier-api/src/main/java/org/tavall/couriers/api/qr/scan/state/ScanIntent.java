/*
 * TJVD License (TJ Valentine’s Discretionary License) — Version 1.0 (2025)
 *
 * Copyright (c) 2025 Taheesh Valentine
 *
 * This source code is protected under the TJVD License.
 * SEE LICENSE.TXT
 */

package org.tavall.couriers.api.qr.scan.state;

public enum ScanIntent {
    NO_UUID_NO_DATA_INTAKE,
    UUID_FOUND_NO_DATA_INTAKE,
    UUID_AND_DATA_FOUND,
    INVALID_SCAN
}