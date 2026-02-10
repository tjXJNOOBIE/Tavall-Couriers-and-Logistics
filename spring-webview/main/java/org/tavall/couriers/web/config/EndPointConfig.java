package org.tavall.couriers.web.config;


import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.tavall.couriers.api.utils.StringCaseUtil;
import org.tavall.couriers.api.web.camera.CameraOptions;
import org.tavall.couriers.api.web.endpoints.camera.CameraFeedEndpoints;
import org.tavall.couriers.api.web.endpoints.config.ClientConfigEndpoints;

import java.util.Arrays;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
public class EndPointConfig {



    @GetMapping(ClientConfigEndpoints.HANDSHAKE_PATH)
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

        Map<String, Object> cameraConfig = Map.of(
                "defaultModeKey", CameraOptions.DEFAULT_MODE_KEY,
                "modes", CameraOptions.modesByKey().entrySet().stream()
                        .collect(Collectors.toMap(
                                Map.Entry::getKey,
                                entry -> entry.getValue().toMap()
                        ))
        );

        return Map.of(
                "endpoints", endpointMap,
                "cameraConfig", cameraConfig,
                "timeouts", Map.of(
                        "pollIntervalIdle", 4000,
                        "pollIntervalActive", 1000
                )
        );
    }
}