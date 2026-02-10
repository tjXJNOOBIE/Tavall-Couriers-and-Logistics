package org.tavall.couriers.api.web.camera;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

public final class CameraOptions {

    public static final String DEFAULT_MODE_KEY = "standardIntake";

    private final CameraType cameraType;
    private final String mode;
    private final boolean intakeFlow;

    private static final CameraOptions MERCHANT_INTAKE = new CameraOptions(CameraType.INTAKE, "merchant-intake", true);
    private static final CameraOptions STANDARD_INTAKE = new CameraOptions(CameraType.INTAKE, "standard-intake", false);
    private static final CameraOptions DRIVER_STATE = new CameraOptions(CameraType.QR_SCAN, "driver-state", false);
    private static final CameraOptions ROUTE_SCANNER = new CameraOptions(CameraType.ROUTE, "route-scanner", false);

    private static final Map<String, CameraOptions> OPTIONS_BY_KEY;
    private static final Map<String, CameraOptions> OPTIONS_BY_MODE;

    static {
        Map<String, CameraOptions> keyMap = new LinkedHashMap<>();
        keyMap.put("merchantIntake", MERCHANT_INTAKE);
        keyMap.put(DEFAULT_MODE_KEY, STANDARD_INTAKE);
        keyMap.put("driverState", DRIVER_STATE);
        keyMap.put("routeScanner", ROUTE_SCANNER);
        OPTIONS_BY_KEY = Collections.unmodifiableMap(keyMap);

        Map<String, CameraOptions> modeMap = new LinkedHashMap<>();
        for (CameraOptions option : OPTIONS_BY_KEY.values()) {
            modeMap.put(option.mode, option);
        }
        OPTIONS_BY_MODE = Collections.unmodifiableMap(modeMap);
    }

    private CameraOptions(CameraType cameraType, String mode, boolean intakeFlow) {
        this.cameraType = cameraType;
        this.mode = mode;
        this.intakeFlow = intakeFlow;
    }

    public CameraType cameraType() {
        return cameraType;
    }

    public String mode() {
        return mode;
    }

    public boolean intakeFlow() {
        return intakeFlow;
    }

    public static Map<String, CameraOptions> modesByKey() {
        return OPTIONS_BY_KEY;
    }

    public static CameraOptions defaultOption() {
        return OPTIONS_BY_KEY.get(DEFAULT_MODE_KEY);
    }

    public static CameraOptions fromMode(String mode) {
        if (mode == null) {
            return defaultOption();
        }
        return OPTIONS_BY_MODE.getOrDefault(mode, defaultOption());
    }

    public static CameraOptions fromKey(String key) {
        if (key == null) {
            return defaultOption();
        }
        return OPTIONS_BY_KEY.getOrDefault(key, defaultOption());
    }

    public Map<String, Object> toMap() {
        return Map.of(
                "type", cameraType.name(),
                "mode", mode,
                "intakeFlow", intakeFlow
        );
    }
}
