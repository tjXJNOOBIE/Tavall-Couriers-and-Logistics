package org.tavall.couriers.web.view.controller.camera;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;
import org.tavall.couriers.api.console.Log;
import org.tavall.couriers.api.qr.scan.metadata.ScanResponse;
import org.tavall.couriers.api.web.camera.CameraOptions;
import org.tavall.couriers.api.web.endpoints.Routes;
import org.tavall.couriers.api.web.service.camera.CameraPageService;
import org.tavall.couriers.api.web.service.camera.CameraScanResult;

@Controller
@PreAuthorize("hasAnyRole('MERCHANT','DRIVER','SUPERUSER')")
public class CameraPageController {

    private final CameraPageService cameraPageService;

    public CameraPageController(CameraPageService cameraPageService) {
        this.cameraPageService = cameraPageService;
    }

    @PostMapping(Routes.CAMERA_STREAM_FRAME)
    public ResponseEntity<ScanResponse> receiveFrame(@RequestParam("image") MultipartFile image,
                                                     @RequestParam(value = "scanMode", required = false) String scanMode,
                                                     @RequestParam(value = "routeId", required = false) String routeId,
                                                     @RequestParam(value = "scanSessionId", required = false) String scanSessionId,
                                                     Authentication authentication) {
        if (image == null || image.isEmpty()) {
            Log.warn("[CameraPage] Empty frame upload rejected.");
            return ResponseEntity.badRequest().body(cameraPageService.errorResponse("Empty frame data"));
        }

        try {
            byte[] snapshot = image.getBytes();
            CameraOptions options = CameraOptions.fromMode(scanMode);
            Log.info("[CameraPage] Frame received (" + snapshot.length + " bytes, mode=" + options.mode() + ").");
            CameraScanResult result = cameraPageService.handleFrame(snapshot, options, authentication, routeId, scanSessionId);
            if (result != null && result.forbidden()) {
                Log.warn("[CameraPage] Scan forbidden for mode=" + options.mode() + ".");
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body(result.response());
            }
            if (result != null && result.response() != null) {
                Log.info("[CameraPage] Scan response returned: state=" + result.response().cameraState());
                return ResponseEntity.ok(result.response());
            }
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(cameraPageService.errorResponse("Failed to analyze frame"));
        } catch (Exception e) {
            Log.exception(e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(cameraPageService.errorResponse("Failed to analyze frame"));
        }
    }

    @PostMapping(Routes.CAMERA_CLOSE_SESSION)
    public ResponseEntity<Void> closeSession(@RequestParam(value = "scanSessionId", required = false) String scanSessionId) {
        cameraPageService.closeSession(scanSessionId);
        return ResponseEntity.ok().build();
    }

    @GetMapping(Routes.CAMERA_INTAKE_STATUS)
    public ResponseEntity<ScanResponse> intakeStatus(@RequestParam(value = "scanSessionId", required = false) String scanSessionId) {
        ScanResponse response = cameraPageService.getIntakeStatus(scanSessionId);
        return ResponseEntity.ok(response);
    }
}
