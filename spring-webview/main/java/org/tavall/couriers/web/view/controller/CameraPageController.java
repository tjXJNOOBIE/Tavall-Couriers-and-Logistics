package org.tavall.couriers.web.view.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import org.tavall.couriers.api.web.endpoints.camera.CameraFeedEndpoints;
import org.tavall.couriers.api.web.endpoints.page.PageViewEndpoints;
import org.tavall.couriers.web.view.GeminiVisionService;
import org.tavall.couriers.api.qr.scan.metadata.ScanResponse;
import org.tavall.couriers.api.qr.scan.state.LiveCameraState;

import java.io.IOException;
import java.time.Instant;

@Controller
public class CameraPageController {

//    private final GeminiVisionService visionService;
//    private final QRDetectorService qrDetector;
//
//    public CameraPageController(GeminiVisionService visionService, QRDetectorService qrDetector) {
//
//        this.visionService = visionService;
//        this.qrDetector = qrDetector;
//    }
//
//
//
//    @PostMapping(value = "/internal/api/v1/stream/frame")
//    public ResponseEntity<?> receiveFrame(@RequestParam("image") MultipartFile image) {
////        if (!stateService.isSystemActive()) return ResponseEntity.status(503).build();
//
//        try {
//            byte[] bytes = image.getBytes();
//
//            // --- STEP 1: THE CHEAP CHECK (Local CPU) ---
//
//            if (!qrDetector.hasQrCode(bytes)) {
//                // Return "SEARCHING" fast so the UI keeps scanning
//                return ResponseEntity.ok(new ScanResponse(null, LiveCameraState.ANALYZING, null, null, null, null, null, null));
//            }
//
//            // --- STEP 2: THE SMART CHECK (Gemini API) ---
//            Gemini3Response<ScanResponse> scanResponse = visionService.analyzeFrame(bytes);
//            if(scanResponse.getResponse() != null) {
//                return ResponseEntity.ok(scanResponse.getResponse());
//            }
//        } catch (IOException e) {
//            return ResponseEntity.internalServerError().build();
//        }
//        return ResponseEntity.ok(new ScanResponse(null, LiveCameraState.ERROR, null, null, null, null, null, "Failed to analyze frame"));
//    }
@GetMapping(PageViewEndpoints.HOME_PATH)
public String home(Model model) {
    model.addAttribute("title", "Thymeleaf One-File Sanity Test");
    model.addAttribute("renderedAt", Instant.now().toString());
    return "home"; // maps to templates/home.html
}

    @PostMapping(CameraFeedEndpoints.STREAM_FRAME_PATH)
    public ResponseEntity<Void> receiveFrame() {
        return ResponseEntity.status(HttpStatus.NOT_IMPLEMENTED).build();
    }

}
