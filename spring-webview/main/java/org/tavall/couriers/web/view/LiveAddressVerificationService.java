package org.tavall.couriers.web.view;

import com.google.genai.types.Content;
import com.google.genai.types.GenerateContentConfig;
import com.google.genai.types.GenerateContentResponse;
import com.google.genai.types.ImageConfig;
import com.google.genai.types.MediaResolution;
import com.google.genai.types.Part;
import com.google.genai.types.Schema;
import com.google.genai.types.Type;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.tavall.couriers.api.console.Log;
import org.tavall.gemini.clients.Gemini3ImageClient;
import org.tavall.gemini.clients.response.enums.ResponseStatus;
import org.tavall.gemini.clients.response.metadata.ClientResponseMetadata;
import org.tavall.gemini.enums.GeminiModel;
import org.tavall.gemini.token.ClientResponseVisualizer;
import org.tavall.gemini.utils.AIResponseParser;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.node.ObjectNode;

import javax.imageio.ImageIO;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.time.Duration;
import java.util.Arrays;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class LiveAddressVerificationService {

    private final ObjectMapper objectMapper = new ObjectMapper();
    private Gemini3ImageClient liveClient;
    private boolean liveEnabled;
    private final Map<String, Integer> liveVerifyAttempts = new ConcurrentHashMap<>();

    @Autowired
    public void initLiveClient() {
        this.liveClient = buildLiveClient();
        this.liveEnabled = this.liveClient != null;
    }

    public AddressCheckResult enforceVerification(ObjectNode objectNode,
                                                  byte[] frameData,
                                                  boolean shouldScanQR,
                                                  boolean documentDetected) {
        if (shouldScanQR || !documentDetected) {
            return AddressCheckResult.SKIPPED;
        }
        if (objectNode == null || frameData == null || frameData.length == 0) {
            Log.warn("[LiveAPI] Missing frame or response payload; skipping verification.");
            return AddressCheckResult.SKIPPED;
        }
        if (!liveEnabled) {
            Log.warn("[LiveAPI] LiveAPI unavailable; skipping address verification.");
            return AddressCheckResult.SKIPPED;
        }

        Log.info("[LiveAPI] Document detected; running address verification.");
        String key = buildSessionKey(objectNode, frameData);
        int attempt = liveVerifyAttempts.getOrDefault(key, 0) + 1;
        liveVerifyAttempts.put(key, attempt);
        Log.info("[LiveAPI] Address verification attempt " + attempt + "/3 (" + key + ").");

        boolean visible = verifyAddressLive(frameData, objectNode);
        if (visible) {
            liveVerifyAttempts.remove(key);
            Log.info("[LiveAPI] Address verified.");
            return AddressCheckResult.VERIFIED;
        }

        if (attempt < 3) {
            objectNode.put("cameraState", "ANALYZING");
            String verifyNote = "Verifying address...";
            if (attempt > 1) {
                verifyNote = "Verifying address... (" + attempt + "/3)";
            }
            objectNode.put("notes", verifyNote);
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
        Log.warn("[LiveAPI] Address verification failed after 3 attempts.");
        return AddressCheckResult.BLOCKED;
    }

    private boolean verifyAddressLive(byte[] frameData, ObjectNode objectNode) {
        try {
            FramePayload payload = prepareLiveFrame(frameData);
            String prompt = buildAddressVisiblePrompt(objectNode);
            Content content = Content.fromParts(
                    Part.fromText(prompt),
                    Part.fromBytes(payload.data(), payload.mimeType())
            );
            long startNanos = System.nanoTime();
            GenerateContentResponse response = liveClient.getClient().models.generateContent(
                    String.valueOf(GeminiModel.GEMINI_3_FLASH), content, buildLiveConfig());
            Duration latency = Duration.ofNanos(System.nanoTime() - startNanos);
            Log.info("[LiveAPI] Response time: " + latency.toMillis() + "ms");
            ClientResponseMetadata metadata = ClientResponseVisualizer.buildMetadata(
                    response,
                    GeminiModel.GEMINI_3_FLASH,
                    latency,
                    ResponseStatus.COMPLETED
            );
            Log.info("[GeminiUsage] LiveAPI " + ClientResponseVisualizer.format(metadata));
            String text = response.text();
            Log.info("[LiveAPI] Response -> " + text);
            return parseAddressVisible(text);
        } catch (Exception ex) {
            Log.warn("[LiveAPI] Address verification failed.");
            Log.exception(ex);
            ClientResponseMetadata metadata = ClientResponseVisualizer.buildMetadata(
                    null,
                    GeminiModel.GEMINI_3_FLASH,
                    Duration.ZERO,
                    ResponseStatus.FAILED
            );
            Log.info("[GeminiUsage] LiveAPI " + ClientResponseVisualizer.format(metadata));
            return false;
        }
    }

    private String buildAddressVisiblePrompt(ObjectNode node) {
        return """
            You validate shipping label visibility. Return JSON {"addressVisible": true|false} only.
            Only return true if the street address, city, state, and zip are clearly present and look real.
            Do not invent data. If anything is missing or blurry, return false.
            Address:
            Street: %s
            City: %s
            State: %s
            Zip: %s
            Country: %s
            """.formatted(
                safe(textField(node, "address")),
                safe(textField(node, "city")),
                safe(textField(node, "state")),
                safe(textField(node, "zipCode")),
                safe(textField(node, "country"))
        );
    }

    private String buildSessionKey(ObjectNode node, byte[] frameData) {
        String addressKey = buildAddressKey(node);
        if (addressKey != null && !addressKey.isBlank()) {
            return "addr:" + addressKey;
        }
        return "frame:" + Integer.toHexString(Arrays.hashCode(frameData));
    }

    private String buildAddressKey(ObjectNode node) {
        if (node == null) {
            return null;
        }
        return String.join("|",
                safe(textField(node, "address")).toUpperCase(Locale.ROOT),
                safe(textField(node, "city")).toUpperCase(Locale.ROOT),
                safe(textField(node, "state")).toUpperCase(Locale.ROOT),
                safe(textField(node, "zipCode")).toUpperCase(Locale.ROOT)
        );
    }

    private boolean parseAddressVisible(String raw) {
        if (raw == null || raw.isBlank()) {
            return false;
        }
        String cleaned = raw;
        if (cleaned.contains("```json")) {
            cleaned = cleaned.replace("```json", "").replace("```", "").trim();
        }
        try {
            JsonNode root = objectMapper.readTree(cleaned);
            JsonNode visibleNode = root.get("addressVisible");
            if (visibleNode != null && !visibleNode.isNull()) {
                return visibleNode.asBoolean(false);
            }
        } catch (Exception ex) {
            Log.warn("[LiveAPI] Address visibility parse fallback engaged: " + ex.getMessage());
        }
        return AIResponseParser.parseBoolean(cleaned, false);
    }

    private Gemini3ImageClient buildLiveClient() {
        String apiKey = System.getenv("GEMINI_API_KEY");
        if (apiKey == null || apiKey.isBlank()) {
            Log.warn("[LiveAPI] LiveAPI client unavailable: GEMINI_API_KEY missing.");
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
            return new FramePayload(frameData, "application/octet-stream");
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
            Log.info("[LiveAPI] Frame resized from " + frameData.length + " -> " + resized.length + " bytes.");
            return new FramePayload(resized, "image/jpeg");
        } catch (Exception ex) {
            return new FramePayload(frameData, resolveMimeType(frameData));
        }
    }

    private BufferedImage loadImage(byte[] frameData) {
        try (ByteArrayInputStream input = new ByteArrayInputStream(frameData)) {
            return ImageIO.read(input);
        } catch (Exception e) {
            return null;
        }
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

    private String safe(String value) {
        return value == null ? "" : value;
    }
}
