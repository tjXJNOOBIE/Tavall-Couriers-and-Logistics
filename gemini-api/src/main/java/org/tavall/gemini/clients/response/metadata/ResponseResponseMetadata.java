package org.tavall.gemini.clients.response.metadata;


import org.tavall.gemini.clients.response.enums.ResponseStatus;
import org.tavall.gemini.enums.GeminiModel;

import java.time.Duration;

public record ResponseResponseMetadata(
        GeminiModel modelUsed,
        int totalTokenCount,
        Duration latency,
        ResponseStatus responseStatus
) {
}