package org.tavall.couriers.api.concurrent;

import java.util.concurrent.StructuredTaskScope;

public record Outcome<T>(
        int index,
        StructuredTaskScope.Subtask.State state,
        T result,
        Throwable error
) {
}
