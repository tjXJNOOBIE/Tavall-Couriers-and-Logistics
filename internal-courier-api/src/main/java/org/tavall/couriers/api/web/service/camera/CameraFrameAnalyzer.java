package org.tavall.couriers.api.web.service.camera;

import org.tavall.couriers.api.qr.scan.metadata.ScanResponse;
import org.tavall.gemini.clients.response.Gemini3Response;

import java.util.concurrent.CompletableFuture;

public interface CameraFrameAnalyzer {
    Gemini3Response<ScanResponse> analyzeFrame(byte[] frameData, boolean shouldScanQR);

    CompletableFuture<Gemini3Response<ScanResponse>> analyzeFrameAsync(byte[] frameData, boolean shouldScanQR);

    boolean looksLikeDocument(byte[] frameData);
}
