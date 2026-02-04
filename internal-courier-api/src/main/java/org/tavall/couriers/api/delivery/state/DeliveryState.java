/*
 * TJVD License (TJ Valentine’s Discretionary License) — Version 1.0 (2025)
 *
 * Copyright (c) 2025 Taheesh Valentine
 *
 * This source code is protected under the TJVD License.
 * SEE LICENSE.TXT
 */

package org.tavall.couriers.api.delivery.state;

public enum DeliveryState {
    LABEL_CREATED, DELIVERED, IN_TRANSIT, IN_HQ, IN_MIDDLEMAN, CANCELLED, ON_HOLD, OUT_FOR_DELIVERY, RETRY
}