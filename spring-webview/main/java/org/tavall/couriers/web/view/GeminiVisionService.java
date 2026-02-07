package org.tavall.couriers.web.view;

import com.google.genai.types.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.tavall.couriers.api.concurrent.AsyncTask;
import org.tavall.couriers.api.console.Log;
import org.tavall.couriers.api.delivery.state.cache.DeliveryStateCache;
import org.tavall.couriers.api.qr.scan.metadata.LocalQRScanData;
import org.tavall.couriers.api.qr.scan.response.ScanResponseSchema;
import org.tavall.couriers.api.qr.scan.LocalQRScanner;
import org.tavall.couriers.api.qr.scan.state.ScanIntent;
import org.tavall.couriers.api.shipping.ShippingLabelMetaData;
import org.tavall.gemini.clients.Gemini3ImageClient;
import org.tavall.gemini.clients.response.Gemini3Response;
import org.tavall.gemini.enums.GeminiModel;
import org.tavall.couriers.api.qr.scan.cache.ScanCacheService;
import org.tavall.couriers.api.qr.scan.metadata.ScanResponse;
import org.tavall.couriers.api.qr.scan.state.LiveCameraState;
import tools.jackson.databind.ObjectMapper;

import java.time.Duration;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

@Service
public class GeminiVisionService {

    private final ObjectMapper objectMapper = new ObjectMapper();
    private Gemini3ImageClient client;
    @Autowired
    private ScanResponseSchema scanResponseSchema;
    @Autowired
    private DeliveryStateCache deliveryStateCache;
    @Autowired
    private ScanCacheService scanCache;
    private ScanResponse scanResponse;
    private static final float RENDER_DPI = 300;
    @Autowired
    private LocalQRScanner localScanner;


    public GeminiVisionService() {

    }


    public Gemini3Response<ScanResponse> analyzeFrame(byte[] frameData, boolean shouldScanQR) {
        //TODO: Currently this creates one client per call
        this.client = new Gemini3ImageClient(scanResponseSchema.getScanResponseSchema());

        try {
            if (frameData == null || frameData.length == 0) {
                // Return empty record with nulls for new fields
                return new Gemini3Response<>(new ScanResponse(null, LiveCameraState.ERROR, null, null, null, null, null, null, null, null, null, "Empty Frame Data"));
            }
            //TODO: Add logic to detect if this ais a first scan
            Optional<UUID> localUuid = localScanner.scanPdfForQrCode(frameData);

            if (localUuid.isPresent()) {
                UUID verifiedId = localUuid.get();
                Log.info("Locally Verified UUID: " + verifiedId.toString());

                // Pass verifiedId.toString() to your hybrid merge logic...
            }
            // Note: You said you'd update the prompt later, so I'm leaving it alone.
            // Just know that until you ask the AI for 'city' and 'state', these fields will be null in the JSON response.
            String promptText = """
                SYSTEM: You scan QR Codes and Shipping Labels.
                TASK: Extract structured data.
                RULES:
                1. If NO label/QR is legible, return cameraState: "ANALYZING".
                2. If a QR Code contains a UUID, extract it and return cameraState: "FOUND".
                3. Extract Recipient Name, Full Address, City, State, Zip, Country, Phone, Tracking Number.
                4. Extract "Deliver By" date/time as ISO-8601 string.
                
                OUTPUT JSON FORMAT (Strict):
                {
                    "uuid": "string or null",
                    "cameraState": "FOUND", "ANALYZING" or "ERROR",
                    "trackingNumber": "string or null",
                    "name": "string or null",
                    "address": "string or null",
                    "city": "string or null",
                    "state": "string or null",
                    "zipCode": "string or null",
                    "country": "string or null",
                    "phoneNumber": "string or null",
                    "deadline": "ISO-8601 string or null",
                    "notes": "string or null"
                }
                """;

            Content content = Content.fromParts(Part.fromText(promptText), Part.fromBytes(frameData, "application/pdf"));
            GenerateContentConfig config = client.getGenerationConfig();
            GenerateContentResponse response = client.getClient().models.generateContent(
                    String.valueOf(GeminiModel.GEMINI_3_FLASH), content, config);

            String jsonText = response.text();
            if (jsonText.contains("```json")) {
                jsonText = jsonText.replace("```json", "").replace("```", "").trim();
            }

            ScanResponse geminiResponse = objectMapper.readValue(jsonText, ScanResponse.class);
            this.scanResponse = geminiResponse;

            if (geminiResponse.cameraState() == LiveCameraState.FOUND) {
                Log.info("[Service] Registering Raw Scan Response");
                // Cache #1: Raw Scan Response
                scanCache.registerScanResponse(geminiResponse);

                // Cache #2: Cached Scan Response
                ShippingLabelMetaData metaData = getShippingLabelMetaData(geminiResponse);
                DeliveryStateCache.get().registerDeliveryState(metaData);
            }

            return new Gemini3Response<>(geminiResponse);

        } catch (Exception e) {
            System.err.println("Gemini Vision Error: " + e.getMessage());
            Log.exception(e);
            // Return empty record with error msg
            return new Gemini3Response<>(new ScanResponse(null, LiveCameraState.ERROR, null, null, null, null, null, null, null, null, null, e.getMessage()));
        }
    }

    private ShippingLabelMetaData getShippingLabelMetaData(ScanResponse geminiResponse) {

        ShippingLabelMetaData metaData = new ShippingLabelMetaData();
        metaData.setUuid(geminiResponse.uuid());
        metaData.setTrackingNumber(geminiResponse.trackingNumber());
        metaData.setRecipientName(geminiResponse.name());
        metaData.setAddress(geminiResponse.address());

        metaData.setCity(geminiResponse.city());
        metaData.setState(geminiResponse.state());
        metaData.setZipCode(geminiResponse.zipCode());
        metaData.setCountry(geminiResponse.country());

        metaData.setPhoneNumber(geminiResponse.phoneNumber());
        metaData.setDeliverBy(geminiResponse.deadline());
        return metaData;
    }


    public CompletableFuture<Gemini3Response<ScanResponse>> analyzeFrameAsync(byte[] imageBytes, boolean shouldScanQR) {
        // Defensive snapshot
        final byte[] snapshot = (imageBytes == null) ? null : imageBytes.clone();

        var opts = AsyncTask.ScopeOptions.defaults()
                .withName("analyze-frame")
                .withTimeout(Duration.ofSeconds(30));

        return AsyncTask.runFuture(() -> analyzeFrame(snapshot, shouldScanQR), opts)
                .exceptionally(ex -> {
                    String msg = AsyncTask.unwrapMessage(ex);
                    // Updated error constructor
                    ScanResponse errorResponse = new ScanResponse(
                            null, LiveCameraState.ERROR, null,
                            null, null, null,
                            null, null, null,
                            null, null, "Async Error: " + msg);
                    return new Gemini3Response<>(errorResponse);
                });
    }

    public LocalQRScanData classifyScan(byte[] frameData) {
        // 1. Run the Local Scanner
        Optional<UUID> localUuid = localScanner.scanPdfForQrCode(frameData);

        // --- SCENARIO A: NO UUID FOUND ---
        if (localUuid.isEmpty()) {
            // Check if it's your special "Empty/Intake" QR code trigger?
            // Let's assume your "Empty QR" scans as a specific string like "TAVALL_NEW_LABEL"
            // If your scanner filters strictly for UUIDs, you might need to relax it to check for this trigger.

            // For now, if no UUID is found, we assume it's either garbage OR an "Intake Init" if that's how your flow works.
            // If your "Intake" scan is literally a blank QR, this logic might need the raw string check.
            return new LocalQRScanData(ScanIntent.INVALID_SCAN, null, null, false);
        }

        UUID scannedId = localUuid.get();

        // --- SCENARIO B: UUID FOUND. CHECK EXISTENCE. ---
        //TODO: Update boolean to use a real cache/data object
        boolean existsInDb = false; // You need this check!

        if (!existsInDb) {
            // STATE 2: "UUID exists, but no data attached"
            return new LocalQRScanData(
                    ScanIntent.UUID_FOUND_NO_DATA_INTAKE,
                    scannedId,
                    scannedId.toString(),
                    false
            );
        } else {
            // STATE 3: "Have both data and meta data"
            return new LocalQRScanData(
                    ScanIntent.UUID_AND_DATA_FOUND,
                    scannedId,
                    scannedId.toString(),
                    true
            );
        }
    }
}