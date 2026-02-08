package org.tavall.couriers.web.view;

import com.google.genai.types.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.tavall.couriers.api.concurrent.AsyncTask;
import org.tavall.couriers.api.console.Log;
import org.tavall.couriers.api.delivery.state.DeliveryState;
import org.tavall.couriers.api.delivery.state.cache.DeliveryStateCache;
import org.tavall.couriers.api.qr.scan.metadata.LocalQRScanData;
import org.tavall.couriers.api.qr.scan.response.ScanResponseSchema;
import org.tavall.couriers.api.qr.scan.LocalQRScanner;
import org.tavall.couriers.api.qr.scan.state.ScanIntent;
import org.tavall.couriers.api.web.entities.ShippingLabelMetaDataEntity;
import org.tavall.couriers.api.web.service.camera.CameraFrameAnalyzer;
import org.tavall.gemini.clients.Gemini3ImageClient;
import org.tavall.gemini.clients.response.Gemini3Response;
import org.tavall.gemini.enums.GeminiModel;
import org.tavall.couriers.api.qr.scan.cache.ScanCacheService;
import org.tavall.couriers.api.qr.scan.metadata.ScanResponse;
import org.tavall.couriers.api.qr.scan.state.CameraState;
import org.tavall.couriers.api.qr.scan.state.GeminiResponseState;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.node.ObjectNode;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

@Service
public class GeminiVisionService implements CameraFrameAnalyzer {

    private final ObjectMapper objectMapper = new ObjectMapper();
    private Gemini3ImageClient client;
    @Autowired
    private ScanResponseSchema scanResponseSchema;
    @Autowired
    private DeliveryStateCache deliveryStateCache;
    @Autowired
    private ScanCacheService scanCache;
    private ScanResponse scanResponse;
    @Autowired
    private LocalQRScanner localScanner;


    public GeminiVisionService() {

    }


    @Override
    public Gemini3Response<ScanResponse> analyzeFrame(byte[] frameData, boolean shouldScanQR) {
        //TODO: Currently this creates one client per call
        this.client = new Gemini3ImageClient(scanResponseSchema.getScanResponseSchema());

        try {
            if (frameData == null || frameData.length == 0) {
                Log.warn("[GeminiVision] Empty frame received.");
                // Return empty record with nulls for new fields
                return new Gemini3Response<>(new ScanResponse(null, CameraState.ERROR, GeminiResponseState.ERROR, null, null, null, null, null, null, null, null, null, "Empty Frame Data"));
            }
            Log.info("[GeminiVision] Frame received (" + frameData.length + " bytes).");
            if (!looksLikeDocument(frameData)) {
                Log.info("[GeminiVision] Document check failed; skipping Gemini call.");
                return new Gemini3Response<>(new ScanResponse(null, CameraState.SEARCHING, GeminiResponseState.IDLE, null, null, null, null, null, null, null, null, null, "No readable document detected"));
            }

            String promptText = buildPrompt(shouldScanQR);
            String mimeType = resolveMimeType(frameData);
            Log.info("[GeminiVision] Sending frame to Gemini (mime=" + mimeType + ", mode=" + (shouldScanQR ? "qr" : "intake") + ").");

            Content content = Content.fromParts(Part.fromText(promptText), Part.fromBytes(frameData, mimeType));
            GenerateContentConfig config = client.getGenerationConfig();
            GenerateContentResponse response = client.getClient().models.generateContent(
                    String.valueOf(GeminiModel.GEMINI_3_FLASH), content, config);

            String jsonText = response.text();
            if (jsonText.contains("```json")) {
                jsonText = jsonText.replace("```json", "").replace("```", "").trim();
            }

            JsonNode root = objectMapper.readTree(jsonText);
            normalizeDeadline(root);
            ScanResponse geminiResponse = withGeminiState(objectMapper.treeToValue(root, ScanResponse.class), GeminiResponseState.COMPLETE);
            this.scanResponse = geminiResponse;

            boolean hasUuid = geminiResponse.uuid() != null && !geminiResponse.uuid().isBlank();
            Log.info("[GeminiVision] Gemini response state=" + geminiResponse.cameraState()
                    + ", uuid=" + geminiResponse.uuid()
                    + ", tracking=" + geminiResponse.trackingNumber());
            if (geminiResponse.cameraState() == CameraState.FOUND && hasUuid) {
                Log.info("[Service] Registering Raw Scan Response");
                // Cache #1: Raw Scan Response
                scanCache.registerScanResponse(geminiResponse);

                // Cache #2: Cached Scan Response
                ShippingLabelMetaDataEntity metaData = getShippingLabelMetaData(geminiResponse);
                deliveryStateCache.registerDeliveryState(metaData);
            }

            return new Gemini3Response<>(geminiResponse);

        } catch (Exception e) {
            System.err.println("Gemini Vision Error: " + e.getMessage());
            Log.exception(e);
            // Return empty record with error msg
            return new Gemini3Response<>(new ScanResponse(null, CameraState.ERROR, GeminiResponseState.ERROR, null, null, null, null, null, null, null, null, null, e.getMessage()));
        }
    }

    private String buildPrompt(boolean shouldScanQR) {
        if (shouldScanQR) {
            return """
                SYSTEM: You scan QR Codes and Shipping Labels.
                TASK: Extract structured data.
                RULES:
                1. If NO label/QR is legible, return cameraState: "SEARCHING".
                2. If a QR Code contains a UUID, extract it and return cameraState: "FOUND".
                3. Extract Recipient Name, Full Address, City, State, Zip, Country, Phone, Tracking Number.
                4. Extract "Deliver By" date/time as ISO-8601 string.
                
                OUTPUT JSON FORMAT (Strict):
                {
                    "uuid": "string or null",
                    "cameraState": "FOUND", "SEARCHING" or "ERROR",
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
        }

        return """
            SYSTEM: You scan full-page shipping documents and intake forms.
            TASK: Extract structured shipment metadata from the entire page, even when there is NO UUID.
            RULES:
            1. If NO label/QR is legible, return cameraState: "SEARCHING".
            2. If a QR Code contains a UUID, extract it and return cameraState: "FOUND".
            3. If a label or form is legible but has NO UUID, return cameraState: "FOUND" and set uuid to null.
            4. Extract Recipient Name, Full Address, City, State, Zip, Country, Phone, Tracking Number.
            5. Extract "Deliver By" date/time as ISO-8601 string.
            6. Prefer data from "Ship To", "Recipient", "Consignee", or "Deliver To" sections.
            7. If a field is missing, return null for that field.
            
            OUTPUT JSON FORMAT (Strict):
            {
                "uuid": "string or null",
                "cameraState": "FOUND", "SEARCHING" or "ERROR",
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
    }

    private ScanResponse withGeminiState(ScanResponse response, GeminiResponseState state) {
        if (response == null) {
            return null;
        }
        return new ScanResponse(
                response.uuid(),
                response.cameraState(),
                state,
                response.trackingNumber(),
                response.name(),
                response.address(),
                response.city(),
                response.state(),
                response.zipCode(),
                response.country(),
                response.phoneNumber(),
                response.deadline(),
                response.notes()
        );
    }

    private String resolveMimeType(byte[] frameData) {
        if (frameData == null || frameData.length < 4) {
            return "application/octet-stream";
        }
        if (isJpeg(frameData)) {
            return "image/jpeg";
        }
        return "image/png";
    }

    private boolean isJpeg(byte[] frameData) {
        return frameData.length >= 3
                && (frameData[0] & 0xFF) == 0xFF
                && (frameData[1] & 0xFF) == 0xD8
                && (frameData[2] & 0xFF) == 0xFF;
    }

    @Override
    public boolean looksLikeDocument(byte[] frameData) {
        if (frameData == null || frameData.length == 0) {
            return false;
        }
        BufferedImage image = loadImage(frameData);
        return imageHasInk(image);
    }

    private BufferedImage loadImage(byte[] frameData) {
        try (ByteArrayInputStream input = new ByteArrayInputStream(frameData)) {
            return ImageIO.read(input);
        } catch (Exception e) {
            return null;
        }
    }

    private boolean imageHasInk(BufferedImage image) {
        if (image == null) {
            return false;
        }
        int width = image.getWidth();
        int height = image.getHeight();
        if (width == 0 || height == 0) {
            return false;
        }
        int step = Math.max(2, Math.min(width, height) / 200);
        int samples = 0;
        int dark = 0;
        for (int y = 0; y < height; y += step) {
            for (int x = 0; x < width; x += step) {
                int rgb = image.getRGB(x, y);
                int r = (rgb >> 16) & 0xFF;
                int g = (rgb >> 8) & 0xFF;
                int b = rgb & 0xFF;
                int luminance = (r * 299 + g * 587 + b * 114) / 1000;
                if (luminance < 210) {
                    dark++;
                }
                samples++;
            }
        }
        if (samples == 0) {
            return false;
        }
        double ratio = (double) dark / (double) samples;
        return ratio >= 0.012;
    }

    private void normalizeDeadline(JsonNode root) {
        if (root == null || !root.has("deadline")) {
            return;
        }
        JsonNode deadlineNode = root.get("deadline");
        if (!deadlineNode.isTextual()) {
            return;
        }
        String raw = deadlineNode.asText();
        if (raw == null || raw.isBlank()) {
            return;
        }
        Instant parsed = parseDeadline(raw);
        if (parsed == null) {
            Log.warn("[GeminiVision] Unable to parse deadline: " + raw);
            return;
        }
        if (root instanceof ObjectNode objectNode) {
            objectNode.put("deadline", parsed.toString());
        }
    }

    private Instant parseDeadline(String raw) {
        String value = raw.trim();
        if (value.isEmpty()) {
            return null;
        }
        try {
            return Instant.parse(value);
        } catch (Exception ignored) {
        }
        try {
            return OffsetDateTime.parse(value).toInstant();
        } catch (Exception ignored) {
        }
        try {
            LocalDateTime parsed = LocalDateTime.parse(value, DateTimeFormatter.ISO_LOCAL_DATE_TIME);
            return parsed.atZone(ZoneId.systemDefault()).toInstant();
        } catch (Exception ignored) {
        }
        try {
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("MM/dd/yyyy hh:mm a", Locale.US);
            LocalDateTime parsed = LocalDateTime.parse(value, formatter);
            return parsed.atZone(ZoneId.systemDefault()).toInstant();
        } catch (Exception ignored) {
        }
        try {
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("M/d/yyyy h:mm a", Locale.US);
            LocalDateTime parsed = LocalDateTime.parse(value, formatter);
            return parsed.atZone(ZoneId.systemDefault()).toInstant();
        } catch (Exception ignored) {
        }
        try {
            LocalDate parsed = LocalDate.parse(value, DateTimeFormatter.ofPattern("MM/dd/yyyy", Locale.US));
            return parsed.atStartOfDay(ZoneId.systemDefault()).toInstant();
        } catch (Exception ignored) {
        }
        return null;
    }

    private ShippingLabelMetaDataEntity getShippingLabelMetaData(ScanResponse geminiResponse) {

        ShippingLabelMetaDataEntity metaData = new ShippingLabelMetaDataEntity();
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
        metaData.setDeliveryState(DeliveryState.LABEL_CREATED);
        return metaData;
    }

    public ShippingLabelMetaDataEntity buildIntakeRequest(ScanResponse scanResponse) {
        ShippingLabelMetaDataEntity request = new ShippingLabelMetaDataEntity();
        if (scanResponse == null) {
            return request;
        }
        request.setRecipientName(scanResponse.name());
        request.setAddress(scanResponse.address());
        request.setCity(scanResponse.city());
        request.setState(scanResponse.state());
        request.setZipCode(scanResponse.zipCode());
        request.setCountry(scanResponse.country());
        request.setPhoneNumber(scanResponse.phoneNumber());
        request.setDeliverBy(scanResponse.deadline());
        request.setDeliveryState(DeliveryState.LABEL_CREATED);
        return request;
    }


    @Override
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
                            null, CameraState.ERROR, GeminiResponseState.ERROR, null,
                            null, null, null,
                            null, null, null,
                            null, null, "Async Error: " + msg);
                    return new Gemini3Response<>(errorResponse);
                });
    }

    public LocalQRScanData classifyScan(byte[] frameData) {
        // 1. Run the Local Scanner
        Optional<UUID> localUuid = localScanner.scanForQrCode(frameData);

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

