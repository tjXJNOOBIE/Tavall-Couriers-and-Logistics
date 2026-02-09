package org.tavall.couriers.api.web.service.route;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.genai.types.Content;
import com.google.genai.types.GenerateContentResponse;
import com.google.genai.types.Part;
import com.google.genai.types.Schema;
import com.google.genai.types.Type;
import org.tavall.couriers.api.console.Log;
import org.tavall.gemini.clients.Gemini3TextClient;
import org.tavall.gemini.enums.GeminiModel;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class GoogleMapsRouteBuilder {

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final Gemini3TextClient client;
    private final boolean geminiEnabled;

    public GoogleMapsRouteBuilder() {
        this.client = buildClient();
        this.geminiEnabled = this.client != null;
    }

    public RouteLinkResult buildRouteLink(List<String> stops) {
        if (stops == null || stops.isEmpty()) {
            return new RouteLinkResult(null, List.of());
        }
        if (stops.size() == 1) {
            return new RouteLinkResult(buildSingleStopUrl(stops.get(0)), List.of(stops.get(0)));
        }

        if (geminiEnabled) {
            try {
                RouteLinkResult result = requestRouteLink(stops);
                if (result != null && result.routeUrl() != null && !result.routeUrl().isBlank()) {
                    return result;
                }
            } catch (Exception ex) {
                Log.warn("Google Maps route build failed, using fallback.");
                Log.exception(ex);
            }
        }

        return buildFallback(stops);
    }

    private RouteLinkResult requestRouteLink(List<String> stops) throws Exception {
        String prompt = buildPrompt(stops);
        Content content = Content.fromParts(Part.fromText(prompt));
        GenerateContentResponse response = client.getClient().models.generateContent(
                String.valueOf(GeminiModel.GEMINI_3_PRO), content, client.getGenerationConfig());

        String jsonText = response.text();
        if (jsonText.contains("```json")) {
            jsonText = jsonText.replace("```json", "").replace("```", "").trim();
        }

        JsonNode root = objectMapper.readTree(jsonText);
        String routeUrl = root.has("routeUrl") ? root.get("routeUrl").asText() : null;
        List<String> orderedStops = new ArrayList<>();
        JsonNode stopsNode = root.get("orderedStops");
        if (stopsNode != null && stopsNode.isArray()) {
            for (JsonNode node : stopsNode) {
                if (node.isTextual()) {
                    orderedStops.add(node.asText());
                }
            }
        }

        return new RouteLinkResult(routeUrl, orderedStops);
    }

    private Gemini3TextClient buildClient() {
        String apiKey = System.getenv("GEMINI_API_KEY");
        if (apiKey == null || apiKey.isBlank()) {
            Log.warn("Gemini route link builder disabled: GEMINI_API_KEY missing.");
            return null;
        }
        return new Gemini3TextClient(buildSchema());
    }

    private RouteLinkResult buildFallback(List<String> stops) {
        String routeUrl = stops != null && stops.size() == 1
                ? buildSingleStopUrl(stops.get(0))
                : buildRouteUrl(stops);
        return new RouteLinkResult(routeUrl, stops);
    }

    private String buildPrompt(List<String> stops) {
        StringBuilder builder = new StringBuilder();
        builder.append("SYSTEM: You are a route planner with access to the Google Maps tool.\n");
        builder.append("TASK: Order the stops into the most efficient driving route.\n");
        builder.append("RULES:\n");
        builder.append("1. Use the Google Maps tool for actual travel time and route order.\n");
        builder.append("2. Preserve the first stop as the origin and the last stop as the destination when possible.\n");
        builder.append("3. Output a Google Maps URL that opens the full route with all stops.\n");
        builder.append("OUTPUT JSON only with fields routeUrl and orderedStops.\n\n");
        builder.append("STOPS:\n");
        for (String stop : stops) {
            builder.append("- ").append(stop).append('\n');
        }
        builder.append("\nOUTPUT FORMAT:\n");
        builder.append("{\"routeUrl\":\"https://www.google.com/maps/dir/...\",\"orderedStops\":[\"stop\"]}\n");
        return builder.toString();
    }

    private String buildRouteUrl(List<String> stops) {
        if (stops == null || stops.size() < 2) {
            return null;
        }
        String origin = encode(stops.get(0));
        String destination = encode(stops.get(stops.size() - 1));
        String waypoints = "";
        if (stops.size() > 2) {
            List<String> mid = stops.subList(1, stops.size() - 1);
            waypoints = String.join("|", mid.stream().map(this::encode).toList());
        }
        StringBuilder url = new StringBuilder("https://www.google.com/maps/dir/?api=1");
        url.append("&origin=").append(origin);
        url.append("&destination=").append(destination);
        if (!waypoints.isBlank()) {
            url.append("&waypoints=").append(waypoints);
        }
        return url.toString();
    }

    private String buildSingleStopUrl(String stop) {
        if (stop == null || stop.isBlank()) {
            return null;
        }
        return "https://www.google.com/maps/search/?api=1&query=" + encode(stop);
    }

    private String encode(String value) {
        return URLEncoder.encode(value == null ? "" : value, StandardCharsets.UTF_8);
    }

    private Schema buildSchema() {
        Map<String, Schema> properties = new HashMap<>();
        properties.put("routeUrl", Schema.builder()
                .type(Type.Known.STRING)
                .build());
        properties.put("orderedStops", Schema.builder()
                .type(Type.Known.ARRAY)
                .items(Schema.builder().type(Type.Known.STRING).build())
                .build());

        return Schema.builder()
                .type(Type.Known.OBJECT)
                .properties(properties)
                .required(List.of("routeUrl", "orderedStops"))
                .build();
    }

    public record RouteLinkResult(String routeUrl, List<String> orderedStops) { }
}
