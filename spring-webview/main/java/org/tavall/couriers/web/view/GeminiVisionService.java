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
import org.tavall.couriers.api.qr.scan.cache.ScanErrorCacheService;
import org.tavall.couriers.api.web.entities.ShippingLabelMetaDataEntity;
import org.tavall.couriers.api.web.service.camera.CameraFrameAnalyzer;
import org.tavall.gemini.clients.Gemini3ImageClient;
import org.tavall.gemini.clients.Gemini3TextClient;
import org.tavall.gemini.clients.response.Gemini3Response;
import org.tavall.gemini.enums.GeminiModel;
import org.tavall.couriers.api.qr.scan.cache.ScanCacheService;
import org.tavall.couriers.api.qr.scan.metadata.ScanResponse;
import org.tavall.couriers.api.qr.scan.state.CameraState;
import org.tavall.couriers.api.qr.scan.state.GeminiResponseState;
import org.tavall.couriers.api.web.service.shipping.ShippingLabelMetaDataService;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.node.ObjectNode;

import javax.imageio.ImageIO;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class GeminiVisionService implements CameraFrameAnalyzer {

    private final ObjectMapper objectMapper = new ObjectMapper();
    private Gemini3ImageClient client;
    private Gemini3ImageClient liveClient;
    private boolean liveEnabled;
    private Gemini3TextClient liteClient;
    private boolean liteEnabled;
    private final Map<String, Integer> liveVerifyAttempts = new ConcurrentHashMap<>();
    @Autowired
    private ScanResponseSchema scanResponseSchema;
    @Autowired
    private DeliveryStateCache deliveryStateCache;
    @Autowired
    private ScanCacheService scanCache;
    @Autowired
    private ScanErrorCacheService scanErrorCache;
    @Autowired
    private ShippingLabelMetaDataService shippingService;
    private ScanResponse scanResponse;
    @Autowired
    private LocalQRScanner localScanner;


    public GeminiVisionService() {

    }

    @Autowired
    public void initLiteClient() {
        this.liteClient = buildLiteClient();
        this.liteEnabled = this.liteClient != null;
        this.liveClient = buildLiveClient();
        this.liveEnabled = this.liveClient != null;
    }


    @Override
    public Gemini3Response<ScanResponse> analyzeFrame(byte[] frameData, boolean shouldScanQR) {
        //TODO: Currently this creates one client per call
        this.client = new Gemini3ImageClient(scanResponseSchema.getScanResponseSchema());

        try {
            if (frameData == null || frameData.length == 0) {
                Log.warn("[GeminiVision] Empty frame received.");
                // Return empty record with nulls for new fields
                return new Gemini3Response<>(new ScanResponse(null, CameraState.ERROR, GeminiResponseState.ERROR, null, null, null, null, null, null, null, null, null, "Empty Frame Data", null, false, false));
            }
            Log.info("[GeminiVision] Frame received (" + frameData.length + " bytes).");
            if (!looksLikeDocument(frameData)) {
                Log.info("[GeminiVision] Document check failed; skipping Gemini call.");
                return new Gemini3Response<>(new ScanResponse(null, CameraState.SEARCHING, GeminiResponseState.IDLE, null, null, null, null, null, null, null, null, null, "No readable document detected", null, false, false));
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
            AddressCheckResult addressCheck = enforceLiveAddressVerification(root, frameData, shouldScanQR);
            if (addressCheck != AddressCheckResult.BLOCKED) {
                applyFunctionCall(root, shouldScanQR);
                enforceIntakeCompleteness(root, shouldScanQR);
            }
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
            if (geminiResponse.cameraState() == CameraState.ERROR
                    && "address_unverified".equalsIgnoreCase(geminiResponse.intakeStatus())) {
                scanErrorCache.registerScanError(geminiResponse);
            }

            return new Gemini3Response<>(geminiResponse);

        } catch (Exception e) {
            System.err.println("Gemini Vision Error: " + e.getMessage());
            Log.exception(e);
            // Return empty record with error msg
            return new Gemini3Response<>(new ScanResponse(null, CameraState.ERROR, GeminiResponseState.ERROR, null, null, null, null, null, null, null, null, null, e.getMessage(), null, false, false));
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
            8. If all required fields are present, prepare a function call to createLabel.
            
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
                "notes": "string or null",
                "functionCall": {
                    "name": "createLabel",
                    "arguments": {
                        "uuid": "string or null",
                        "trackingNumber": "string or null",
                        "name": "string or null",
                        "address": "string or null",
                        "city": "string or null",
                        "state": "string or null",
                        "zipCode": "string or null",
                        "country": "string or null",
                        "phoneNumber": "string or null",
                        "deadline": "ISO-8601 string or null"
                    }
                }
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
                response.notes(),
                response.intakeStatus(),
                response.pendingIntake(),
                response.existingLabel()
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
        return imageLooksDocument(image);
    }

    private BufferedImage loadImage(byte[] frameData) {
        try (ByteArrayInputStream input = new ByteArrayInputStream(frameData)) {
            return ImageIO.read(input);
        } catch (Exception e) {
            return null;
        }
    }

    private boolean imageLooksDocument(BufferedImage image) {
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
        int edge = 0;
        double mean = 0;
        double m2 = 0;
        int lastLum = -1;
        for (int y = 0; y < height; y += step) {
            for (int x = 0; x < width; x += step) {
                int rgb = image.getRGB(x, y);
                int r = (rgb >> 16) & 0xFF;
                int g = (rgb >> 8) & 0xFF;
                int b = rgb & 0xFF;
                int luminance = (r * 299 + g * 587 + b * 114) / 1000;
                if (lastLum >= 0 && Math.abs(luminance - lastLum) > 25) {
                    edge++;
                }
                lastLum = luminance;

                double delta = luminance - mean;
                mean += delta / Math.max(1, samples + 1);
                m2 += delta * (luminance - mean);
                if (luminance < 210) {
                    dark++;
                }
                samples++;
            }
        }
        if (samples == 0) {
            return false;
        }
        double darkRatio = (double) dark / (double) samples;
        double edgeRatio = (double) edge / (double) samples;
        double variance = m2 / (double) samples;
        return darkRatio >= 0.012 && (edgeRatio >= 0.01 || variance >= 150);
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

    private void applyFunctionCall(JsonNode root, boolean shouldScanQR) {
        if (root == null || shouldScanQR) {
            return;
        }
        if (!(root instanceof ObjectNode objectNode)) {
            return;
        }

        JsonNode functionCall = objectNode.get("functionCall");
        if (functionCall == null || !functionCall.isObject()) {
            objectNode.remove("functionCall");
            return;
        }

        String cameraState = objectNode.path("cameraState").asText("");
        if (!"FOUND".equalsIgnoreCase(cameraState)) {
            objectNode.remove("functionCall");
            return;
        }

        String functionName = functionCall.path("name").asText("");
        if (!"createLabel".equalsIgnoreCase(functionName)) {
            objectNode.remove("functionCall");
            return;
        }

        JsonNode args = functionCall.path("arguments");
        if (args == null || !args.isObject()) {
            objectNode.remove("functionCall");
            return;
        }

        if (!hasRequiredArgs(args)) {
            Log.warn("[GeminiVision] Function call missing required args.");
            objectNode.put("cameraState", "SEARCHING");
            objectNode.put("notes", "Incomplete intake metadata.");
            objectNode.remove("functionCall");
            return;
        }

        String uuidArg = textArg(args, "uuid");
        String trackingNumberArg = textArg(args, "trackingNumber");
        Log.info("[GeminiVision] Function call detected for createLabel (uuid=" + uuidArg + ", tracking=" + trackingNumberArg + ").");

        queueAddressVerification(objectNode, args, uuidArg, trackingNumberArg);
        objectNode.remove("functionCall");
    }

    private void queueAddressVerification(ObjectNode objectNode, JsonNode args, String uuidArg, String trackingNumberArg) {
        if (!liteEnabled) {
            Log.warn("[GeminiVision] Gemini 2 Flash Lite unavailable; proceeding without address verification.");
            LabelCreationResult created = createLabelFromArgs(args, uuidArg, trackingNumberArg);
            if (created != null && created.entity() != null) {
                hydrateObjectNodeFromEntity(objectNode, created.entity(), created.existing());
            }
            return;
        }

        objectNode.put("cameraState", "ANALYZING");
        objectNode.put("notes", "Generating\u2026 give us a few seconds..");
        objectNode.put("pendingIntake", true);
        objectNode.put("intakeStatus", "verifying-address");
        objectNode.put("existingLabel", false);

        ShippingLabelMetaDataEntity request = buildEntityFromArgs(args);
        var opts = AsyncTask.ScopeOptions.defaults()
                .withName("address-lite-verify")
                .withTimeout(Duration.ofSeconds(20));

        AsyncTask.runFuture(() -> {
            Log.info("[GeminiVision] AddressLite: verifying address visibility for intake.");
            boolean visible = verifyAddressVisible(request);
            if (!visible) {
                Log.warn("[GeminiVision] AddressLite: address not verified; queuing for rescan.");
                ScanResponse res = new ScanResponse(
                        uuidArg,
                        CameraState.SEARCHING,
                        GeminiResponseState.IDLE,
                        trackingNumberArg,
                        request.getRecipientName(),
                        request.getAddress(),
                        request.getCity(),
                        request.getState(),
                        request.getZipCode(),
                        request.getCountry(),
                        request.getPhoneNumber(),
                        request.getDeliverBy(),
                        "Address not verified. Please rescan.",
                        "address_unverified",
                        false,
                        false
                );
                scanErrorCache.registerScanError(res);
                return null;
            }

            LabelCreationResult created = createLabelFromArgs(args, uuidArg, trackingNumberArg);
            if (created != null && created.entity() != null) {
                Log.success("[GeminiVision] AddressLite: label created after verification -> " + created.entity().getUuid());
                ScanResponse res = new ScanResponse(
                        created.entity().getUuid(),
                        CameraState.FOUND,
                        GeminiResponseState.COMPLETE,
                        created.entity().getTrackingNumber(),
                        created.entity().getRecipientName(),
                        created.entity().getAddress(),
                        created.entity().getCity(),
                        created.entity().getState(),
                        created.entity().getZipCode(),
                        created.entity().getCountry(),
                        created.entity().getPhoneNumber(),
                        created.entity().getDeliverBy(),
                        "Label created after address verification",
                        "verified",
                        false,
                        created.existing()
                );
                scanCache.registerScanResponse(res);
                deliveryStateCache.registerDeliveryState(getShippingLabelMetaData(res));
            }
            return null;
        }, opts);
    }

    private ShippingLabelMetaDataEntity buildEntityFromArgs(JsonNode args) {
        ShippingLabelMetaDataEntity request = new ShippingLabelMetaDataEntity();
        request.setRecipientName(textArg(args, "name"));
        request.setAddress(textArg(args, "address"));
        request.setCity(textArg(args, "city"));
        request.setState(textArg(args, "state"));
        request.setZipCode(textArg(args, "zipCode"));
        request.setCountry(textArg(args, "country"));
        request.setPhoneNumber(textArg(args, "phoneNumber"));
        request.setDeliverBy(parseDeadline(textArg(args, "deadline")));
        request.setDeliveryState(DeliveryState.LABEL_CREATED);
        return request;
    }

    private LabelCreationResult createLabelFromArgs(JsonNode args, String uuidArg, String trackingNumberArg) {
        ShippingLabelMetaDataEntity request = buildEntityFromArgs(args);

        ShippingLabelMetaDataEntity existing = null;
        if (!isBlank(uuidArg)) {
            existing = shippingService.findByUuid(uuidArg);
        }
        if (existing == null && !isBlank(trackingNumberArg)) {
            existing = shippingService.findByTrackingNumber(trackingNumberArg);
        }

        ShippingLabelMetaDataEntity created;
        boolean alreadyExists = existing != null;
        if (alreadyExists) {
            created = existing;
            Log.info("[GeminiVision] Scan matched existing shipment: " + existing.getUuid());
        } else if (uuidArg != null && !uuidArg.isBlank()) {
            created = shippingService.createShipmentWithUuid(request, uuidArg, DeliveryState.LABEL_CREATED);
        } else {
            created = shippingService.createShipment(request, DeliveryState.LABEL_CREATED);
        }

        return new LabelCreationResult(created, alreadyExists);
    }

    private void hydrateObjectNodeFromEntity(ObjectNode objectNode, ShippingLabelMetaDataEntity entity, boolean alreadyExists) {
        if (objectNode == null || entity == null) {
            return;
        }
        objectNode.put("uuid", entity.getUuid());
        objectNode.put("trackingNumber", entity.getTrackingNumber());
        objectNode.put("name", entity.getRecipientName());
        objectNode.put("address", entity.getAddress());
        objectNode.put("city", entity.getCity());
        objectNode.put("state", entity.getState());
        objectNode.put("zipCode", entity.getZipCode());
        objectNode.put("country", entity.getCountry());
        objectNode.put("phoneNumber", entity.getPhoneNumber());
        if (entity.getDeliverBy() != null) {
            objectNode.put("deadline", entity.getDeliverBy().toString());
        }
        objectNode.put("existingLabel", alreadyExists);
        objectNode.put("pendingIntake", !alreadyExists);
        objectNode.put("intakeStatus", alreadyExists ? "existing" : "pending");
    }

    private boolean verifyAddressVisible(ShippingLabelMetaDataEntity request) {
        if (request == null || !liteEnabled) {
            return true;
        }
        try {
            String prompt = buildAddressVisiblePrompt(request);
            Content content = Content.fromParts(Part.fromText(prompt));
            GenerateContentResponse response = liteClient.getClient().models.generateContent(
                    String.valueOf(GeminiModel.GEMINI_2_FLASH_LITE), content, liteClient.getGenerationConfig());
            String text = response.text();
            Log.info("[GeminiVision] AddressLite raw response -> " + text);
            if (text.contains("```json")) {
                text = text.replace("```json", "").replace("```", "").trim();
            }
            JsonNode root = objectMapper.readTree(text);
            JsonNode visibleNode = root.get("addressVisible");
            return visibleNode != null && visibleNode.asBoolean(false);
        } catch (Exception ex) {
            Log.warn("[GeminiVision] AddressLite verification failed, defaulting to allow.");
            Log.exception(ex);
            return true;
        }
    }

    private String buildAddressVisiblePrompt(ShippingLabelMetaDataEntity request) {
        return """
            You validate shipping label visibility. Return JSON {"addressVisible": true|false}.
            Only return true if the street address, city, state, and zip are clearly present and look real.
            Do not invent data. If anything is missing or blurry, return false.
            Address:
            Street: %s
            City: %s
            State: %s
            Zip: %s
            Country: %s
            """.formatted(
                safe(request.getAddress()),
                safe(request.getCity()),
                safe(request.getState()),
                safe(request.getZipCode()),
                safe(request.getCountry())
        );
    }

    private Gemini3TextClient buildLiteClient() {
        String apiKey = System.getenv("GEMINI_API_KEY");
        if (apiKey == null || apiKey.isBlank()) {
            Log.warn("[GeminiVision] Gemini 2 Flash Lite unavailable: GEMINI_API_KEY missing.");
            return null;
        }
        Schema schema = Schema.builder()
                .type(Type.Known.OBJECT)
                .properties(Map.of(
                        "addressVisible", Schema.builder().type(Type.Known.BOOLEAN).build()
                ))
                .required(java.util.List.of("addressVisible"))
                .build();
        return new Gemini3TextClient(schema);
    }

    private AddressCheckResult enforceLiveAddressVerification(JsonNode root, byte[] frameData, boolean shouldScanQR) {
        if (shouldScanQR || frameData == null || frameData.length == 0) {
            return AddressCheckResult.SKIPPED;
        }
        if (!(root instanceof ObjectNode objectNode)) {
            return AddressCheckResult.SKIPPED;
        }
        String cameraState = objectNode.path("cameraState").asText("");
        if (!"FOUND".equalsIgnoreCase(cameraState)) {
            return AddressCheckResult.SKIPPED;
        }
        if (isBlank(textField(objectNode, "address"))) {
            return AddressCheckResult.SKIPPED;
        }
        if (!liveEnabled) {
            Log.warn("[GeminiVision] LiveAPI unavailable; skipping address verification.");
            return AddressCheckResult.SKIPPED;
        }

        String key = buildAddressKey(objectNode);
        int attempt = liveVerifyAttempts.getOrDefault(key, 0) + 1;
        liveVerifyAttempts.put(key, attempt);

        boolean visible = verifyAddressLive(frameData);
        if (visible) {
            liveVerifyAttempts.remove(key);
            return AddressCheckResult.VERIFIED;
        }

        if (attempt < 3) {
            objectNode.put("cameraState", "ANALYZING");
            objectNode.put("notes", "Verifying address (" + attempt + "/3)");
            objectNode.put("intakeStatus", "address_verifying");
            objectNode.put("pendingIntake", true);
            objectNode.remove("functionCall");
            return AddressCheckResult.BLOCKED;
        }

        objectNode.put("cameraState", "ERROR");
        objectNode.put("notes", "Address not verified. Rescan.");
        objectNode.put("intakeStatus", "address_unverified");
        objectNode.put("pendingIntake", false);
        objectNode.remove("functionCall");
        liveVerifyAttempts.remove(key);
        return AddressCheckResult.BLOCKED;
    }

    private boolean verifyAddressLive(byte[] frameData) {
        try {
            FramePayload payload = prepareLiveFrame(frameData);
            String prompt = """
                Confirm if the shipping address is clearly visible. Return JSON {"addressVisible": true|false} only.
                """;
            Content content = Content.fromParts(
                    Part.fromText(prompt),
                    Part.fromBytes(payload.data(), payload.mimeType())
            );
            GenerateContentResponse response = liveClient.getClient().models.generateContent(
                    String.valueOf(GeminiModel.GEMINI_3_FLASH), content, buildLiveConfig());
            String text = response.text();
            Log.info("[GeminiVision] LiveAPI response -> " + text);
            if (text.contains("```json")) {
                text = text.replace("```json", "").replace("```", "").trim();
            }
            JsonNode root = objectMapper.readTree(text);
            JsonNode visibleNode = root.get("addressVisible");
            return visibleNode != null && visibleNode.asBoolean(false);
        } catch (Exception ex) {
            Log.warn("[GeminiVision] LiveAPI address verification failed.");
            Log.exception(ex);
            return false;
        }
    }

    private String buildAddressKey(ObjectNode node) {
        return String.join("|",
                safe(textField(node, "address")).toUpperCase(Locale.ROOT),
                safe(textField(node, "city")).toUpperCase(Locale.ROOT),
                safe(textField(node, "state")).toUpperCase(Locale.ROOT),
                safe(textField(node, "zipCode")).toUpperCase(Locale.ROOT)
        );
    }

    private Gemini3ImageClient buildLiveClient() {
        String apiKey = System.getenv("GEMINI_API_KEY");
        if (apiKey == null || apiKey.isBlank()) {
            Log.warn("[GeminiVision] LiveAPI client unavailable: GEMINI_API_KEY missing.");
            return null;
        }
        Schema schema = Schema.builder()
                .type(Type.Known.OBJECT)
                .properties(Map.of(
                        "addressVisible", Schema.builder().type(Type.Known.BOOLEAN).build()
                ))
                .required(java.util.List.of("addressVisible"))
                .build();
        return new Gemini3ImageClient(schema);
    }

    private GenerateContentConfig buildLiveConfig() {
        return GenerateContentConfig.builder()
                .temperature(0.0f)
                .maxOutputTokens(128)
                .candidateCount(1)
                .imageConfig(ImageConfig.builder().build())
                .mediaResolution(MediaResolution.Known.MEDIA_RESOLUTION_MEDIUM)
                .responseSchema(liveClient.getSchema())
                .responseMimeType("application/json")
                .build();
    }

    private FramePayload prepareLiveFrame(byte[] frameData) {
        if (frameData == null || frameData.length == 0) {
            return new FramePayload(frameData, resolveMimeType(frameData));
        }
        BufferedImage image = loadImage(frameData);
        if (image == null) {
            return new FramePayload(frameData, resolveMimeType(frameData));
        }
        int width = image.getWidth();
        if (width <= 640) {
            return new FramePayload(frameData, resolveMimeType(frameData));
        }
        int height = image.getHeight();
        int targetWidth = 640;
        int targetHeight = Math.max(1, Math.round(targetWidth * (height / (float) width)));
        BufferedImage scaled = new BufferedImage(targetWidth, targetHeight, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = scaled.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        g.drawImage(image, 0, 0, targetWidth, targetHeight, null);
        g.dispose();

        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            ImageIO.write(scaled, "jpg", out);
            byte[] resized = out.toByteArray();
            Log.info("[GeminiVision] LiveAPI frame resized from " + frameData.length + " -> " + resized.length + " bytes.");
            return new FramePayload(resized, "image/jpeg");
        } catch (Exception ex) {
            return new FramePayload(frameData, resolveMimeType(frameData));
        }
    }

    private record FramePayload(byte[] data, String mimeType) { }

    private enum AddressCheckResult {
        VERIFIED,
        SKIPPED,
        BLOCKED
    }

    private void enforceIntakeCompleteness(JsonNode root, boolean shouldScanQR) {
        if (root == null || shouldScanQR || !(root instanceof ObjectNode objectNode)) {
            return;
        }

        String cameraState = objectNode.path("cameraState").asText("");
        if (!"FOUND".equalsIgnoreCase(cameraState)) {
            return;
        }

        boolean hasRequired = hasRequiredFields(objectNode);
        boolean tagged = objectNode.path("pendingIntake").asBoolean(false)
                || objectNode.path("existingLabel").asBoolean(false);

        if (!hasRequired || !tagged) {
            objectNode.put("cameraState", "SEARCHING");
            objectNode.put("notes", "Incomplete intake metadata.");
            objectNode.put("pendingIntake", false);
            objectNode.put("existingLabel", false);
            objectNode.put("intakeStatus", "incomplete");
        }
    }

    private boolean hasRequiredFields(ObjectNode node) {
        return !isBlank(textField(node, "name"))
                && !isBlank(textField(node, "address"))
                && !isBlank(textField(node, "city"))
                && !isBlank(textField(node, "state"))
                && !isBlank(textField(node, "zipCode"))
                && !isBlank(textField(node, "country"));
    }

    private String textField(ObjectNode node, String field) {
        if (node == null || field == null) {
            return null;
        }
        JsonNode val = node.get(field);
        if (val == null || !val.isTextual()) {
            return null;
        }
        return val.asText();
    }

    private boolean hasRequiredArgs(JsonNode args) {
        return !isBlank(textArg(args, "name"))
                && !isBlank(textArg(args, "address"))
                && !isBlank(textArg(args, "city"))
                && !isBlank(textArg(args, "state"))
                && !isBlank(textArg(args, "zipCode"))
                && !isBlank(textArg(args, "country"));
    }

    private String textArg(JsonNode args, String field) {
        if (args == null || field == null) {
            return null;
        }
        JsonNode node = args.get(field);
        if (node == null || !node.isTextual()) {
            return null;
        }
        return node.asText();
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    private String safe(String value) {
        return value == null ? "" : value;
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
                            null, null, "Async Error: " + msg,
                            null, false, false);
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

    private record LabelCreationResult(ShippingLabelMetaDataEntity entity, boolean existing) { }
}