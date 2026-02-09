package org.tavall.couriers.api.web.service.route;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.genai.types.Content;
import com.google.genai.types.GenerateContentResponse;
import com.google.genai.types.Part;
import com.google.genai.types.Schema;
import com.google.genai.types.Type;
import org.springframework.stereotype.Service;
import org.tavall.couriers.api.console.Log;
import org.tavall.couriers.api.qr.scan.cache.ScanErrorCacheService;
import org.tavall.couriers.api.qr.scan.metadata.ScanResponse;
import org.tavall.couriers.api.qr.scan.state.CameraState;
import org.tavall.couriers.api.qr.scan.state.GeminiResponseState;
import org.tavall.couriers.api.web.entities.ShippingLabelMetaDataEntity;
import org.tavall.gemini.clients.Gemini3TextClient;
import org.tavall.gemini.enums.GeminiModel;



import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Service
public class RoutePlannerService {

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final Gemini3TextClient client;
    private final Gemini3TextClient flashClient;
    private final boolean geminiEnabled;
    private final boolean geminiFlashEnabled;
    private final ScanErrorCacheService scanErrorCache;

    public RoutePlannerService(ScanErrorCacheService scanErrorCache) {
        this.client = buildClient();
        this.flashClient = buildFlashClient();
        this.geminiEnabled = this.client != null;
        this.geminiFlashEnabled = this.flashClient != null;
        this.scanErrorCache = scanErrorCache;
    }

    public RoutePlan planRoute(List<ShippingLabelMetaDataEntity> labels) {
        return planRoute(labels, 50.0, 30);
    }

    public RoutePlan planRoute(List<ShippingLabelMetaDataEntity> labels,
                               double radiusMiles,
                               int maxStops) {
        if (labels == null || labels.isEmpty()) {
            return new RoutePlan(List.of(), "No labels to route.");
        }

        Log.info("AddressGuard: starting route plan for " + labels.size() + " labels (radius=" + radiusMiles + ", maxStops=" + maxStops + ")");

        boolean addressesValid = addressGuard(labels);
        if (!addressesValid) {
            Log.warn("AddressGuard: validation failed; skipping Gemini 3 Pro route build and falling back.");
            registerRouteAddressErrors(labels);
            return fallbackPlan(labels, maxStops);
        }

        if (geminiEnabled) {
            try {
                RoutePlan plan = requestRoutePlan(labels, radiusMiles, maxStops);
                if (plan != null && plan.orderedUuids() != null && !plan.orderedUuids().isEmpty()) {
                    Log.info("AddressGuard: Gemini 3 Pro route build succeeded.");
                    return plan;
                }
            } catch (Exception e) {
                Log.warn("Route planner Gemini failed, using fallback ordering.");
                Log.exception(e);
            }
        }

        return fallbackPlan(labels, maxStops);
    }

    private Gemini3TextClient buildClient() {
        String apiKey = System.getenv("GEMINI_API_KEY");
        if (apiKey == null || apiKey.isBlank()) {
            Log.warn("Gemini route planner disabled: GEMINI_API_KEY missing.");
            return null;
        }
        return new Gemini3TextClient(buildRoutePlanSchema());
    }

    private Gemini3TextClient buildFlashClient() {
        String apiKey = System.getenv("GEMINI_API_KEY");
        if (apiKey == null || apiKey.isBlank()) {
            Log.warn("AddressGuard: Gemini 3 Flash guard disabled (API key missing).");
            return null;
        }
        return new Gemini3TextClient(buildAddressGuardSchema());
    }

    private RoutePlan requestRoutePlan(List<ShippingLabelMetaDataEntity> labels,
                                       double radiusMiles,
                                       int maxStops) throws Exception {
        String prompt = buildPrompt(labels, radiusMiles, maxStops);
        Log.info("Route planner Gemini 3 Pro request (radius=" + radiusMiles + ", maxStops=" + maxStops + ")");
        Content content = Content.fromParts(Part.fromText(prompt));
        GenerateContentResponse response = client.getClient().models.generateContent(
                String.valueOf(GeminiModel.GEMINI_3_PRO), content, client.getGenerationConfig());

        String jsonText = response.text();
        if (jsonText.contains("```json")) {
            jsonText = jsonText.replace("```json", "").replace("```", "").trim();
        }

        JsonNode root = objectMapper.readTree(jsonText);
        List<String> ordered = new ArrayList<>();
        JsonNode orderNode = root.get("orderedUuids");
        if (orderNode != null && orderNode.isArray()) {
            for (JsonNode node : orderNode) {
                if (node.isTextual()) {
                    ordered.add(node.asText());
                }
            }
        }

        String notes = root.has("notes") && root.get("notes").isTextual()
                ? root.get("notes").asText()
                : "Gemini route plan";

        return new RoutePlan(ordered, notes);
    }

    private RoutePlan fallbackPlan(List<ShippingLabelMetaDataEntity> labels, int maxStops) {
        List<ShippingLabelMetaDataEntity> ordered = new ArrayList<>(labels);
        ordered.sort(Comparator
                .comparing(ShippingLabelMetaDataEntity::getDeliverBy, Comparator.nullsLast(Comparator.naturalOrder()))
                .thenComparing(label -> normalize(label.getCity()))
                .thenComparing(label -> normalize(label.getState())));

        List<String> uuids = new ArrayList<>();
        for (ShippingLabelMetaDataEntity label : ordered) {
            if (label != null && label.getUuid() != null) {
                if (uuids.size() >= maxStops) {
                    break;
                }
                uuids.add(label.getUuid());
            }
        }

        return new RoutePlan(uuids, "Fallback order by deadline and location.");
    }

    private String normalize(String value) {
        if (value == null) {
            return "";
        }
        return value.trim().toLowerCase(Locale.ROOT);
    }

    private String buildPrompt(List<ShippingLabelMetaDataEntity> labels, double radiusMiles, int maxStops) {
        StringBuilder builder = new StringBuilder();
        builder.append("SYSTEM: You are a route optimization agent with access to the Google Maps tool. ");
        builder.append("TASK: Order shipment UUIDs into the most efficient delivery route using real-world travel time. ");
        builder.append("Only use shipments that fit within a ").append(radiusMiles).append(" mile radius of each other. ");
        builder.append("Do not include more than ").append(maxStops).append(" containers/stops. ");
        builder.append("RULES:\n");
        builder.append("1. Use the Google Maps tool to estimate travel time and distance between stops.\n");
        builder.append("2. Prioritize deadlines; earliest deliverBy first when conflicts exist.\n");
        builder.append("3. If only one valid address remains within the radius, create a route with that single shipment.\n");
        builder.append("4. When deadlines tie, minimize total travel time and avoid backtracking.\n");
        builder.append("5. If address data is incomplete, fall back to city/state proximity.\n");
        builder.append("6. Add labels within the radius to the route that best aligns with deadline and proximity, then order stops accordingly.\n");
        builder.append("7. Add a concise summary in notes with total ETA and any notable constraints.\n");
        builder.append("OUTPUT JSON with orderedUuids array (respect radius and max stops) and notes string.\n\n");
        builder.append("SHIPMENTS:\n");
        for (ShippingLabelMetaDataEntity label : labels) {
            if (label == null) {
                continue;
            }
            builder.append("- uuid: ").append(label.getUuid()).append('\n');
            builder.append("  address: ").append(safe(label.getAddress())).append('\n');
            builder.append("  city: ").append(safe(label.getCity())).append('\n');
            builder.append("  state: ").append(safe(label.getState())).append('\n');
            builder.append("  zip: ").append(safe(label.getZipCode())).append('\n');
            builder.append("  deadline: ").append(formatInstant(label.getDeliverBy())).append('\n');
        }
        builder.append("\nOUTPUT FORMAT:\n");
        builder.append("{\"orderedUuids\": [\"uuid\"], \"notes\": \"string\"}\n");
        return builder.toString();
    }

    private boolean addressGuard(List<ShippingLabelMetaDataEntity> labels) {
        if (!geminiFlashEnabled) {
            Log.info("AddressGuard: Flash guard unavailable; allowing route build.");
            return true;
        }
        try {
            Log.info("AddressGuard: invoking Gemini 3 Flash guard for validation");
            String prompt = buildAddressGuardPrompt(labels);
            Content content = Content.fromParts(Part.fromText(prompt));
            GenerateContentResponse response = flashClient.getClient().models.generateContent(
                    String.valueOf(GeminiModel.GEMINI_3_FLASH), content, flashClient.getGenerationConfig());
            String text = response.text();
            Log.info("AddressGuard: Flash guard response -> " + text);

            boolean valid = parseAddressGuard(text);
            if (valid) {
                Log.info("AddressGuard: Flash guard validated addresses; triggering intake for routing.");
            } else {
                Log.warn("AddressGuard: Flash guard flagged invalid address data; aborting Pro route build.");
                registerRouteAddressErrors(labels);
            }
            return valid;
        } catch (Exception ex) {
            Log.warn("AddressGuard: Lite validation failed, defaulting to allow route build.");
            Log.exception(ex);
            return true;
        }
    }

    private void registerRouteAddressErrors(List<ShippingLabelMetaDataEntity> labels) {
        if (labels == null || labels.isEmpty() || scanErrorCache == null) {
            return;
        }
        for (ShippingLabelMetaDataEntity label : labels) {
            if (label == null) {
                continue;
            }
            boolean missingAddress = isBlank(label.getAddress())
                    || isBlank(label.getCity())
                    || isBlank(label.getState())
                    || isBlank(label.getZipCode());
            if (!missingAddress) {
                continue;
            }
            ScanResponse response = new ScanResponse(
                    label.getUuid(),
                    CameraState.ERROR,
                    GeminiResponseState.ERROR,
                    label.getTrackingNumber(),
                    label.getRecipientName(),
                    label.getAddress(),
                    label.getCity(),
                    label.getState(),
                    label.getZipCode(),
                    label.getCountry(),
                    label.getPhoneNumber(),
                    label.getDeliverBy(),
                    "Routing address missing. Rescan or edit the label.",
                    "routing_address_missing",
                    false,
                    false
            );
            scanErrorCache.registerScanError(response);
        }
    }

    private String buildAddressGuardPrompt(List<ShippingLabelMetaDataEntity> labels) {
        StringBuilder builder = new StringBuilder();
        builder.append("You are an address validator with access to the Google Maps tool. ")
                .append("Verify each address is real and complete before routing using the Google Maps tool. ")
                .append("Do NOT build routes. Use function call startIntakeForRouting only. ")
                .append("If addresses look real, set valid=true in the function call so we can start the route shipment intake process. ")
                .append("If any address is missing or fake, set valid=false.\n");
        builder.append("ADDRESSES:\n");
        for (ShippingLabelMetaDataEntity label : labels) {
            if (label == null) continue;
            builder.append("- address: ").append(safe(label.getAddress())).append('\n');
            builder.append("  city: ").append(safe(label.getCity())).append('\n');
            builder.append("  state: ").append(safe(label.getState())).append('\n');
            builder.append("  zip: ").append(safe(label.getZipCode())).append('\n');
        }
        builder.append("Respond via function call only.");
        return builder.toString();
    }

    private boolean parseAddressGuard(String jsonText) throws Exception {
        if (jsonText == null || jsonText.isBlank()) {
            return false;
        }
        if (jsonText.contains("```json")) {
            jsonText = jsonText.replace("```json", "").replace("```", "").trim();
        }
        JsonNode root = objectMapper.readTree(jsonText);
        if (root.has("valid")) {
            return root.get("valid").asBoolean(false);
        }
        if (root.has("function") && root.get("function").asText("").equalsIgnoreCase("startIntakeForRouting")) {
            JsonNode args = root.get("arguments");
            if (args != null && args.has("valid")) {
                return args.get("valid").asBoolean(false);
            }
            return true;
        }
        return false;
    }

    private Schema buildAddressGuardSchema() {
        Map<String, Schema> args = new HashMap<>();
        args.put("valid", Schema.builder()
                .type(Type.Known.BOOLEAN)
                .description("Whether the address set is valid for routing intake")
                .build());

        Map<String, Schema> function = new HashMap<>();
        function.put("function", Schema.builder()
                .type(Type.Known.STRING)
                .build());
        function.put("arguments", Schema.builder()
                .type(Type.Known.OBJECT)
                .properties(args)
                .build());

        return Schema.builder()
                .type(Type.Known.OBJECT)
                .properties(function)
                .required(List.of("function"))
                .build();
    }

    private String safe(String value) {
        return value == null ? "" : value.trim();
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    private String formatInstant(Instant value) {
        return value == null ? "" : value.toString();
    }

    private Schema buildRoutePlanSchema() {
        Map<String, Schema> properties = new HashMap<>();
        properties.put("orderedUuids", Schema.builder()
                .type(Type.Known.ARRAY)
                .items(Schema.builder().type(Type.Known.STRING).build())
                .build());
        properties.put("notes", Schema.builder()
                .type(Type.Known.STRING)
                .nullable(true)
                .build());

        return Schema.builder()
                .type(Type.Known.OBJECT)
                .properties(properties)
                .required(List.of("orderedUuids"))
                .build();
    }
}
