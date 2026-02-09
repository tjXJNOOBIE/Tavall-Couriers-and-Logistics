package org.tavall.couriers.api.qr.scan.session;

import org.tavall.couriers.api.qr.scan.state.CameraState;
import org.tavall.couriers.api.web.camera.CameraType;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public final class CameraSession {

    private final String sessionId;
    private final Set<String> scannedKeys = ConcurrentHashMap.newKeySet();
    private volatile CameraType cameraType;
    private volatile CameraState lastState;
    private volatile long lastTouchedAt;

    public CameraSession(String sessionId, CameraType cameraType) {
        this.sessionId = sessionId;
        this.cameraType = cameraType;
        touch();
    }

    public String sessionId() {
        return sessionId;
    }

    public CameraType cameraType() {
        return cameraType;
    }

    public CameraState lastState() {
        return lastState;
    }

    public boolean hasKey(String key) {
        touch();
        return scannedKeys.contains(key);
    }

    public void addKey(String key) {
        touch();
        scannedKeys.add(key);
    }

    public void updateState(CameraState state) {
        touch();
        this.lastState = state;
    }

    public void updateCameraType(CameraType type) {
        touch();
        if (type != null) {
            this.cameraType = type;
        }
    }

    public long lastTouchedAt() {
        return lastTouchedAt;
    }

    private void touch() {
        lastTouchedAt = System.currentTimeMillis();
    }
}
