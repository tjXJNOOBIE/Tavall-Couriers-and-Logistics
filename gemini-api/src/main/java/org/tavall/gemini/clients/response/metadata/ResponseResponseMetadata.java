package org.tavall.gemini.clients.response.metadata;


import org.tavall.gemini.clients.response.enums.ResponseStatus;

import java.time.Duration;

public record ResponseResponseMetadata(
        String modelUsed,
        int totalTokenCount,
        Duration latency,
        ResponseStatus responseStatus
) {
}