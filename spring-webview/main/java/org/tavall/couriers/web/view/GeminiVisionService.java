package org.tavall.couriers.web.view;

import com.google.genai.types.*;
import org.springframework.stereotype.Service;
import org.tavall.couriers.api.concurrent.AsyncTask;
import org.tavall.couriers.api.console.Log;
import org.tavall.couriers.api.intake.driver.scanner.ai.schemas.ScanResponseSchema;
import org.tavall.couriers.web.beans.ScanCacheServiceBean;
import org.tavall.couriers.web.beans.ScanResponseSchemaBean;
import org.tavall.gemini.clients.Gemini3ImageClient;
import org.tavall.gemini.clients.response.Gemini3Response;
import org.tavall.gemini.enums.GeminiModel;
import org.tavall.springapi.service.cache.ScanCacheService;
import org.tavall.springapi.scan.metadata.ScanResponse;
import org.tavall.springapi.scan.state.LiveCameraState;
import tools.jackson.databind.ObjectMapper;

import java.time.Duration;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Service
public class GeminiVisionService {

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final Gemini3ImageClient client;
    private ScanResponseSchema scanResponseSchema;
    private ScanCacheService scanCache = ScanCacheService.INSTANCE;
    private Schema schema;
    private static ScanResponse scanResponse;

    public GeminiVisionService() {
        this.scanResponseSchema = new ScanResponseSchema();
        this.client = new Gemini3ImageClient(scanResponseSchema.getScanResponseSchema());
    }


    public Gemini3Response<ScanResponse> analyzeFrame(byte[] imageBytes) {
        try {
            // 1. Fail Fast
            if (imageBytes == null || imageBytes.length == 0) {
                return new Gemini3Response<>(new ScanResponse(null, LiveCameraState.ERROR, null, null, null, null, null, "Empty Frame Data"));
            }

            // 2. The Schema-Driven Prompt
            // FIX: "SEARCHING" -> "ANALYZING" to match your Enum and prevent Jackson Crash
            String promptText = """
                SYSTEM: You scan QR Codes and Page Data.
                TASK: Extract data from the shipping label or QR code.
                RULES:
                1. If NO label/QR is legible, return cameraState: "ANALYZING".
                2. If a QR Code contains a UUID, extract it and return cameraState: "FOUND".
                3. If QR is present but invalid/empty, return "null" for uuid.
                4. Extract Recipient Name, Address, Phone, Tracking Number.
                5. Extract "Deliver By" date as ISO-8601 deadline.
                
                OUTPUT JSON FORMAT (Strict):
                {
                    "uuid": "string or null",
                    "cameraState": "FOUND", "ANALYZING" or "ERROR",
                    "trackingNumber": "string or null",
                    "name": "string or null",
                    "address": "string or null",
                    "phoneNumber": "string or null",
                    "deadline": "ISO-8601 string or null",
                    "notes": "string or null"
                }
                """;

            Content content = Content.fromParts(
                    Part.fromText(promptText),
                    Part.fromBytes(imageBytes, "image/png"));

            GenerateContentConfig config = client.getGenerationConfig(); // Ensure this method exists

            GenerateContentResponse response = client.getClient().models.generateContent(
                    String.valueOf(GeminiModel.GEMINI_3_FLASH), content, config);

            String jsonText = response.text();
            if (jsonText.contains("```json")) {
                jsonText = jsonText.replace("```json", "").replace("```", "").trim();
            }
            ScanResponse geminiResponse = objectMapper.readValue(jsonText, ScanResponse.class);

            Gemini3Response<ScanResponse> responseWrapper = new Gemini3Response<>(geminiResponse);
            ScanResponse scanData = responseWrapper.getResponse();
            scanResponse = geminiResponse;
            // 3. INLINE CACHE (The "Hook")
            // Only cache if we actually found something valid
            if (scanData.cameraState() == LiveCameraState.FOUND && scanData.uuid() != null) {
                Log.info("[Service] Caching UUID: " + scanData.uuid());
                scanCache.registerScanResponse(scanResponse);

            }

            // 4. RETURN
            return new Gemini3Response<>(scanData);

        } catch (Exception e) {
            System.err.println("Gemini Vision Error: " + e.getMessage());
            // Return ERROR state so frontend handles it gracefully
            return new Gemini3Response<>(new ScanResponse(null, LiveCameraState.ERROR, null, null, null, null, null, e.getMessage()));
        }
    }

    public CompletableFuture<Gemini3Response<ScanResponse>> analyzeFrameAsync(byte[] imageBytes) {
        // Defensive snapshot
        final byte[] snapshot = (imageBytes == null) ? null : imageBytes.clone();

        var opts = AsyncTask.ScopeOptions.defaults()
                .withName("analyze-frame")
                .withTimeout(Duration.ofSeconds(30));

        return AsyncTask.runFuture(() -> analyzeFrame(snapshot), opts)
                .exceptionally(ex -> {
                    String msg = AsyncTask.unwrapMessage(ex);
                    // FIX: Return a valid Object with ERROR state, not NULL.
                    // This fixes the NPE in your Test.

                    ScanResponse errorResponse = new ScanResponse(
                            null,
                            LiveCameraState.ERROR,
                            null, null, null, null, null,
                            "Async Error: " + msg
                    );
                    return new Gemini3Response<>(errorResponse);
                });
    }

    public static ScanResponse getScanResponse(){
        return scanResponse;
    }

}