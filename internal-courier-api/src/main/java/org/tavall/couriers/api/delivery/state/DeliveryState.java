/*
 * TJVD License (TJ Valentine’s Discretionary License) — Version 1.0 (2025)
 *
 * Copyright (c) 2025 Taheesh Valentine
 *
 * This source code is protected under the TJVD License.
 * SEE LICENSE.TXT
 */

package org.tavall.couriers.api.delivery.state;

import java.util.EnumSet;

public enum DeliveryState {
    LABEL_CREATED,
    DELIVERED,
    IN_TRANSIT,
    IN_HQ,
    IN_MIDDLEMAN,
    CANCELLED,
    ON_HOLD,
    OUT_FOR_DELIVERY,
    RETRY;

    public EnumSet<DeliveryState> allowedTransitions() {
        return switch (this) {
            case LABEL_CREATED -> EnumSet.of(IN_TRANSIT, IN_HQ, ON_HOLD, CANCELLED);
            case IN_HQ -> EnumSet.of(IN_TRANSIT, IN_MIDDLEMAN, ON_HOLD, CANCELLED);
            case IN_MIDDLEMAN -> EnumSet.of(IN_TRANSIT, ON_HOLD, CANCELLED);
            case IN_TRANSIT -> EnumSet.of(OUT_FOR_DELIVERY, IN_HQ, IN_MIDDLEMAN, ON_HOLD, RETRY, CANCELLED);
            case OUT_FOR_DELIVERY -> EnumSet.of(DELIVERED, RETRY, ON_HOLD, CANCELLED);
            case RETRY -> EnumSet.of(OUT_FOR_DELIVERY, IN_TRANSIT, ON_HOLD, CANCELLED);
            case ON_HOLD -> EnumSet.of(IN_TRANSIT, IN_HQ, IN_MIDDLEMAN, OUT_FOR_DELIVERY, CANCELLED);
            case DELIVERED, CANCELLED -> EnumSet.noneOf(DeliveryState.class);
        };
    }

    public boolean canTransitionTo(DeliveryState nextState) {
        return nextState != null && allowedTransitions().contains(nextState);
    }

    public String displayName() {
        return switch (this) {
            case LABEL_CREATED -> "Label Created";
            case DELIVERED -> "Delivered";
            case IN_TRANSIT -> "In Transit";
            case IN_HQ -> "In HQ";
            case IN_MIDDLEMAN -> "In Partner Hub";
            case CANCELLED -> "Cancelled";
            case ON_HOLD -> "On Hold";
            case OUT_FOR_DELIVERY -> "Out for Delivery";
            case RETRY -> "Retry";
        };
    }
}
