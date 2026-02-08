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
    private final boolean geminiEnabled;

    public RoutePlannerService() {
        this.client = buildClient();
        this.geminiEnabled = this.client != null;
    }

    public RoutePlan planRoute(List<ShippingLabelMetaDataEntity> labels) {
        if (labels == null || labels.isEmpty()) {
            return new RoutePlan(List.of(), "No labels to route.");
        }

        if (geminiEnabled) {
            try {
                RoutePlan plan = requestRoutePlan(labels);
                if (plan != null && plan.orderedUuids() != null && !plan.orderedUuids().isEmpty()) {
                    return plan;
                }
            } catch (Exception e) {
                Log.warn("Route planner Gemini failed, using fallback ordering.");
                Log.exception(e);
            }
        }

        return fallbackPlan(labels);
    }

    private Gemini3TextClient buildClient() {
        String apiKey = System.getenv("GEMINI_API_KEY");
        if (apiKey == null || apiKey.isBlank()) {
            Log.warn("Gemini route planner disabled: GEMINI_API_KEY missing.");
            return null;
        }
        return new Gemini3TextClient(buildRoutePlanSchema());
    }

    private RoutePlan requestRoutePlan(List<ShippingLabelMetaDataEntity> labels) throws Exception {
        String prompt = buildPrompt(labels);
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

    private RoutePlan fallbackPlan(List<ShippingLabelMetaDataEntity> labels) {
        List<ShippingLabelMetaDataEntity> ordered = new ArrayList<>(labels);
        ordered.sort(Comparator
                .comparing(ShippingLabelMetaDataEntity::getDeliverBy, Comparator.nullsLast(Comparator.naturalOrder()))
                .thenComparing(label -> normalize(label.getCity()))
                .thenComparing(label -> normalize(label.getState())));

        List<String> uuids = new ArrayList<>();
        for (ShippingLabelMetaDataEntity label : ordered) {
            if (label != null && label.getUuid() != null) {
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

    private String buildPrompt(List<ShippingLabelMetaDataEntity> labels) {
        StringBuilder builder = new StringBuilder();
        builder.append("SYSTEM: You are a route optimization agent with access to the Google Maps tool. ");
        builder.append("TASK: Order shipment UUIDs into the most efficient delivery route using real-world travel time. ");
        builder.append("RULES:\n");
        builder.append("1. Use the Google Maps tool to estimate travel time and distance between stops.\n");
        builder.append("2. Prioritize deadlines; earliest deliverBy first when conflicts exist.\n");
        builder.append("3. When deadlines tie, minimize total travel time and avoid backtracking.\n");
        builder.append("4. If address data is incomplete, fall back to city/state proximity.\n");
        builder.append("OUTPUT JSON with orderedUuids array and notes string.\n\n");
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

    private String safe(String value) {
        return value == null ? "" : value.trim();
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
