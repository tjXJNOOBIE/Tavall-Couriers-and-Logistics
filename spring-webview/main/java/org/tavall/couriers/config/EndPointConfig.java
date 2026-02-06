package org.tavall.couriers.config;


import org.springframework.web.bind.annotation.GetMapping;
import org.tavall.couriers.api.utils.StringCaseUtil;
import org.tavall.couriers.api.web.endpoints.camera.CameraFeedEndpoints;

import java.util.Arrays;
import java.util.Map;
import java.util.stream.Collectors;

public class EndPointConfig {



    @GetMapping("/internal/api/v1/config/handshake")
    public Map<String, Object> getClientConfig() {

        // 1. DYNAMICALLY MAP ENUMS
        // Converts CameraFeedEndpoints.STREAM_FRAME -> "streamFrame": "/internal/api/v1/stream/frame"
        Map<String, String> endpointMap = Arrays.stream(CameraFeedEndpoints.values())
                .collect(Collectors.toMap(
                        enumValue -> StringCaseUtil.toCamelCase(enumValue.name()), // Key: "streamFrame"
                        CameraFeedEndpoints::endpoint               // Value: "/path/to/api"
                ));

        // 2. Add System Control Endpoints (If you make an Enum for these later, use that too!)
        endpointMap.put("systemStatus", "/internal/api/v1/control/status");
        endpointMap.put("systemToggle", "/internal/api/v1/control/toggle");

        return Map.of(
                "endpoints", endpointMap,
                "timeouts", Map.of(
                        "pollIntervalIdle", 4000,
                        "pollIntervalActive", 1000
                )
        );
    }
}