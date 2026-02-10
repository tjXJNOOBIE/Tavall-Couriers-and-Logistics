/*
 * TJVD License (TJ Valentine's Discretionary License) - Version 1.0 (2025)
 *
 * Copyright (c) 2025 Taheesh Valentine
 *
 * This source code is protected under the TJVD License.
 * SEE LICENSE.TXT
 */

package org.tavall.couriers.api.qr.scan.state;

public enum GeminiResponseState {
    IDLE,
    RESPONDING,
    COMPLETE,
    ERROR

    ;

    public String displayName() {
        return switch (this) {
            case IDLE -> "Idle";
            case RESPONDING -> "Responding";
            case COMPLETE -> "Complete";
            case ERROR -> "Error";
        };
    }
}
