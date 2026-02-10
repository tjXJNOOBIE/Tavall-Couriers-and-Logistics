package org.tavall.couriers.api.qr.scan.session;

import org.springframework.stereotype.Service;
import org.tavall.couriers.api.console.Log;
import org.tavall.couriers.api.qr.scan.metadata.ScanResponse;
import org.tavall.couriers.api.qr.scan.state.CameraState;
import org.tavall.couriers.api.web.camera.CameraType;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class CameraSessionService {

    private static final long SESSION_TTL_MS = 20 * 60 * 1000;
    private static final String KEY_UUID = "uuid:";
    private static final String KEY_TRACKING = "tracking:";
    private static final String KEY_ADDRESS = "addr:";
    private final Map<String, CameraSession> sessions = new ConcurrentHashMap<>();

    public void updateState(String sessionId, CameraType cameraType, CameraState state) {
        CameraSession session = getSession(sessionId, cameraType);
        if (session == null) {
            return;
        }
        session.updateState(state);
    }

    public boolean isDuplicate(String sessionId, CameraType cameraType, String uuid, String trackingNumber, String addressKey) {
        if (isBlank(sessionId)) {
            return false;
        }
        cleanupExpired();
        CameraSession session = getSession(sessionId, cameraType);
        if (session == null) {
            return false;
        }
        if (!isBlank(uuid) && session.hasKey(KEY_UUID + uuid)) {
            return true;
        }
        if (!isBlank(trackingNumber) && session.hasKey(KEY_TRACKING + trackingNumber)) {
            return true;
        }
        return !isBlank(addressKey) && session.hasKey(KEY_ADDRESS + addressKey);
    }

    public boolean isDuplicateAddress(String sessionId, CameraType cameraType, String addressKey) {
        if (isBlank(sessionId) || isBlank(addressKey)) {
            return false;
        }
        cleanupExpired();
        CameraSession session = getSession(sessionId, cameraType);
        return session != null && session.hasKey(KEY_ADDRESS + addressKey);
    }

    public void registerScan(String sessionId, CameraType cameraType, ScanResponse response, String addressKey) {
        if (response == null || isBlank(sessionId)) {
            return;
        }
        registerScan(sessionId, cameraType, response.uuid(), response.trackingNumber(), addressKey);
    }

    public void registerScan(String sessionId, CameraType cameraType, String uuid, String trackingNumber, String addressKey) {
        if (isBlank(sessionId)) {
            return;
        }
        cleanupExpired();
        CameraSession session = getSession(sessionId, cameraType);
        if (session == null) {
            return;
        }
        if (!isBlank(uuid)) {
            session.addKey(KEY_UUID + uuid);
        }
        if (!isBlank(trackingNumber)) {
            session.addKey(KEY_TRACKING + trackingNumber);
        }
        if (!isBlank(addressKey)) {
            session.addKey(KEY_ADDRESS + addressKey);
        }
    }

    public void closeSession(String sessionId) {
        if (isBlank(sessionId)) {
            return;
        }
        CameraSession removed = sessions.remove(sessionId);
        if (removed != null) {
            Log.info("[CameraSession] Closed session " + sessionId);
        }
    }

    private CameraSession getSession(String sessionId, CameraType cameraType) {
        if (isBlank(sessionId)) {
            return null;
        }
        CameraSession session = sessions.computeIfAbsent(sessionId, id -> new CameraSession(id, cameraType));
        if (cameraType != null) {
            session.updateCameraType(cameraType);
        }
        return session;
    }

    private void cleanupExpired() {
        long now = System.currentTimeMillis();
        sessions.entrySet().removeIf(entry -> now - entry.getValue().lastTouchedAt() > SESSION_TTL_MS);
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
