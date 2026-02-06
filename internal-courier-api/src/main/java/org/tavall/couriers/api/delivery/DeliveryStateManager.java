package org.tavall.couriers.api.delivery;


import org.tavall.couriers.api.delivery.state.DeliveryState;

import java.util.*;

public class DeliveryStateManager {
    private static final Map<DeliveryState, Set<DeliveryState>> ALLOWED_TRANSITIONS = buildAllowedTransitions();

    private DeliveryState currentState;
    private final List<DeliveryState> history;
    //TODO: Better scope the override logic
    public DeliveryStateManager() {
        this(DeliveryState.LABEL_CREATED);
    }

    public DeliveryStateManager(DeliveryState initialState) {
        if (initialState == null) {
            throw new IllegalArgumentException("Initial delivery state cannot be null.");
        }
        this.currentState = initialState;
        this.history = new ArrayList<>();
        this.history.add(initialState);
    }

    public DeliveryState getCurrentState() {
        return currentState;
    }

    public List<DeliveryState> getHistory() {
        return Collections.unmodifiableList(history);
    }

    public boolean isTerminalState() {
        return currentState == DeliveryState.DELIVERED || currentState == DeliveryState.CANCELLED;
    }

    // Normal check (no override)
    public boolean canTransitionTo(DeliveryState nextState) {
        return canTransitionTo(nextState, null);
    }

    // Scoped check (override only applies to this call)
    public boolean canTransitionTo(DeliveryState nextState, Map<DeliveryState, Set<DeliveryState>> overrides) {
        if (nextState == null) {
            return false;
        }
        if (currentState == nextState) {
            return false;
        }

        Set<DeliveryState> allowed = getAllowedNextStatesInternal(overrides);
        return allowed.contains(nextState);
    }

    // Normal transition (no override)
    public boolean transitionTo(DeliveryState nextState) {
        return transitionTo(nextState, null);
    }

    // Scoped transition (override only applies to this call)
    public boolean transitionTo(DeliveryState nextState, Map<DeliveryState, Set<DeliveryState>> overrides) {
        if (nextState == null) {
            throw new IllegalArgumentException("Next delivery state cannot be null.");
        }
        if (currentState == nextState) {
            return false;
        }

        if (isTerminalState() && (overrides == null || overrides.isEmpty())) {
            throw new IllegalStateException(String.format(
                    "Cannot transition from terminal delivery state %s.", currentState));
        }

        if (!canTransitionTo(nextState, overrides)) {
            throw new IllegalStateException(String.format(
                    "Cannot transition delivery state from %s to %s.", currentState, nextState));
        }

        this.currentState = nextState;
        this.history.add(nextState);
        return true;
    }

    // Normal allowed-next-states (no override)
    public Set<DeliveryState> getAllowedNextStates() {
        return getAllowedNextStates(null);
    }

    // Scoped allowed-next-states (override only applies to this call)
    public Set<DeliveryState> getAllowedNextStates(Map<DeliveryState, Set<DeliveryState>> overrides) {
        Set<DeliveryState> allowed = getAllowedNextStatesInternal(overrides);
        return allowed.isEmpty() ? Collections.emptySet() : Collections.unmodifiableSet(allowed);
    }

    private Set<DeliveryState> getAllowedNextStatesInternal(Map<DeliveryState, Set<DeliveryState>> overrides) {
        Set<DeliveryState> base = ALLOWED_TRANSITIONS.get(currentState);
        Set<DeliveryState> result = (base == null || base.isEmpty())
                ? EnumSet.noneOf(DeliveryState.class)
                : EnumSet.copyOf(base);

        if (overrides == null || overrides.isEmpty()) {
            return result;
        }

        Set<DeliveryState> extra = overrides.get(currentState);
        if (extra == null || extra.isEmpty()) {
            return result;
        }

        // Merge behavior: override ADDS to defaults, it does not replace them.
        result.addAll(EnumSet.copyOf(extra));
        return result;
    }

    private static Map<DeliveryState, Set<DeliveryState>> buildAllowedTransitions() {
        Map<DeliveryState, Set<DeliveryState>> transitions = new EnumMap<>(DeliveryState.class);

        transitions.put(DeliveryState.LABEL_CREATED,
                EnumSet.of(DeliveryState.IN_TRANSIT, DeliveryState.IN_HQ, DeliveryState.ON_HOLD, DeliveryState.CANCELLED));

        transitions.put(DeliveryState.IN_HQ,
                EnumSet.of(DeliveryState.IN_TRANSIT, DeliveryState.IN_MIDDLEMAN, DeliveryState.ON_HOLD, DeliveryState.CANCELLED));

        transitions.put(DeliveryState.IN_MIDDLEMAN,
                EnumSet.of(DeliveryState.IN_TRANSIT, DeliveryState.ON_HOLD, DeliveryState.CANCELLED));

        transitions.put(DeliveryState.IN_TRANSIT,
                EnumSet.of(DeliveryState.OUT_FOR_DELIVERY, DeliveryState.IN_HQ, DeliveryState.IN_MIDDLEMAN,
                        DeliveryState.ON_HOLD, DeliveryState.RETRY, DeliveryState.CANCELLED));

        transitions.put(DeliveryState.OUT_FOR_DELIVERY,
                EnumSet.of(DeliveryState.DELIVERED, DeliveryState.RETRY, DeliveryState.ON_HOLD, DeliveryState.CANCELLED));

        transitions.put(DeliveryState.RETRY,
                EnumSet.of(DeliveryState.OUT_FOR_DELIVERY, DeliveryState.IN_TRANSIT, DeliveryState.ON_HOLD, DeliveryState.CANCELLED));

        transitions.put(DeliveryState.ON_HOLD,
                EnumSet.of(DeliveryState.IN_TRANSIT, DeliveryState.IN_HQ, DeliveryState.IN_MIDDLEMAN,
                        DeliveryState.OUT_FOR_DELIVERY, DeliveryState.CANCELLED));

        transitions.put(DeliveryState.DELIVERED, EnumSet.noneOf(DeliveryState.class));
        transitions.put(DeliveryState.CANCELLED, EnumSet.noneOf(DeliveryState.class));

        return Collections.unmodifiableMap(transitions);
    }
}