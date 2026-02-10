package org.tavall.couriers.web.view;

import com.google.genai.types.Content;
import com.google.genai.types.GenerateContentResponse;
import com.google.genai.types.Part;
import com.google.genai.types.Schema;
import com.google.genai.types.Type;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.tavall.couriers.api.console.Log;
import org.tavall.couriers.api.web.entities.ShippingLabelMetaDataEntity;
import org.tavall.gemini.clients.Gemini3TextClient;
import org.tavall.gemini.clients.response.enums.ResponseStatus;
import org.tavall.gemini.clients.response.metadata.ClientResponseMetadata;
import org.tavall.gemini.enums.GeminiModel;
import org.tavall.gemini.token.ClientResponseVisualizer;
import org.tavall.gemini.utils.AIResponseParser;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.time.Duration;
import java.util.List;
import java.util.Map;

@Service
public class ManualAddressVerificationService {

    private static final String MISSING_API_KEY_MESSAGE = "[ManualAddress] Gemini 3 Flash unavailable: GEMINI_API_KEY missing.";
    private final ObjectMapper objectMapper = new ObjectMapper();
    private Gemini3TextClient client;
    private boolean enabled;

    @Autowired
    public void initClient() {
        this.client = buildClient();
        this.enabled = this.client != null;
    }

    public ManualAddressCheckResult checkAddressForIntake(ShippingLabelMetaDataEntity request) {
        return checkAddress(request, false);
    }

    public boolean isKnownAddress(ShippingLabelMetaDataEntity request) {
        ManualAddressCheckResult result = checkAddress(request, true);
        return result != ManualAddressCheckResult.UNKNOWN;
    }

    private Gemini3TextClient buildClient() {
        String apiKey = System.getenv("GEMINI_API_KEY");
        if (apiKey == null || apiKey.isBlank()) {
            return null;
        }
        Schema schema = Schema.builder()
                .type(Type.Known.OBJECT)
                .properties(Map.of(
                        "known", Schema.builder().type(Type.Known.BOOLEAN).build(),
                        "reason", Schema.builder().type(Type.Known.STRING).nullable(true).build()
                ))
                .required(List.of("known"))
                .build();
        return new Gemini3TextClient(schema);
    }

    private ManualAddressCheckResult checkAddress(ShippingLabelMetaDataEntity request, boolean allowExternalTools) {
        if (request == null) {
            return ManualAddressCheckResult.FAILED;
        }
        if (!enabled) {
            Log.warn(MISSING_API_KEY_MESSAGE);
            return ManualAddressCheckResult.FAILED;
        }
        if (isBlank(request.getAddress())
                || isBlank(request.getCity())
                || isBlank(request.getState())
                || isBlank(request.getZipCode())) {
            Log.warn("[ManualAddress] Missing address fields; blocking manual label creation.");
            return ManualAddressCheckResult.UNKNOWN;
        }

        try {
            String prompt = buildPrompt(request, allowExternalTools);
            Content content = Content.fromParts(Part.fromText(prompt));
            long startNanos = System.nanoTime();
            GenerateContentResponse response;
            try {
                response = client.getClient().models.generateContent(
                        String.valueOf(GeminiModel.GEMINI_3_FLASH), content, client.getGenerationConfig());
            } catch (Exception ex) {
                ClientResponseMetadata metadata = ClientResponseVisualizer.buildMetadata(
                        null,
                        GeminiModel.GEMINI_3_FLASH,
                        Duration.ofNanos(System.nanoTime() - startNanos),
                        ResponseStatus.FAILED
                );
                Log.info("[GeminiUsage] ManualAddress " + ClientResponseVisualizer.format(metadata));
                throw ex;
            }
            ClientResponseMetadata metadata = ClientResponseVisualizer.buildMetadata(
                    response,
                    GeminiModel.GEMINI_3_FLASH,
                    Duration.ofNanos(System.nanoTime() - startNanos),
                    ResponseStatus.COMPLETED
            );
            Log.info("[GeminiUsage] ManualAddress " + ClientResponseVisualizer.format(metadata));
            String text = response.text();
            Log.info("[ManualAddress] Flash response -> " + text);
            boolean known = parseKnown(text);
            return known ? ManualAddressCheckResult.VERIFIED : ManualAddressCheckResult.UNKNOWN;
        } catch (Exception ex) {
            Log.warn("[ManualAddress] Verification failed.");
            Log.exception(ex);
            ClientResponseMetadata metadata = ClientResponseVisualizer.buildMetadata(
                    null,
                    GeminiModel.GEMINI_3_FLASH,
                    Duration.ZERO,
                    ResponseStatus.FAILED
            );
            Log.info("[GeminiUsage] ManualAddress " + ClientResponseVisualizer.format(metadata));
            return ManualAddressCheckResult.FAILED;
        }
    }

    private String buildPrompt(ShippingLabelMetaDataEntity request, boolean allowExternalTools) {
        String toolLine = allowExternalTools
                ? "You validate shipping addresses using the Google Maps tool."
                : "You validate shipping addresses without external tools.";
        return """
            %s
            Return JSON {"known": true|false, "reason": "string"} only.
            Only return known=true if the street, city, state, and zip match a real address.
            Do not invent data. If anything is missing or unclear, return known=false.
            Address:
            Street: %s
            City: %s
            State: %s
            Zip: %s
            Country: %s
            """.formatted(
                toolLine,
                safe(request.getAddress()),
                safe(request.getCity()),
                safe(request.getState()),
                safe(request.getZipCode()),
                safe(request.getCountry())
        );
    }

    private boolean parseKnown(String raw) {
        if (raw == null || raw.isBlank()) {
            return false;
        }
        String cleaned = raw;
        if (cleaned.contains("```json")) {
            cleaned = cleaned.replace("```json", "").replace("```", "").trim();
        }
        try {
            JsonNode root = objectMapper.readTree(cleaned);
            JsonNode knownNode = root.get("known");
            if (knownNode != null && !knownNode.isNull()) {
                return knownNode.asBoolean(false);
            }
        } catch (Exception ex) {
            Log.warn("[ManualAddress] Response parse fallback engaged: " + ex.getMessage());
        }
        return AIResponseParser.parseBoolean(cleaned, false);
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    private String safe(String value) {
        return value == null ? "" : value.trim();
    }
}
