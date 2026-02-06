package org.tavall.gemini;


import com.google.genai.Client;
import com.google.genai.types.*;
import org.tavall.gemini.enums.GeminiAPIVersion;
import org.tavall.gemini.enums.GeminiModel;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

public class GeminiAPI{

    String GEMINI_PROJECT = "Tavall";
    public GeminiAPI() {

    }

    public void generateResponse(String model, String prompt, boolean hasConfig){

        Client client = Client.builder().apiKey
                (System.getenv("GEMINI_API_KEY"))
                .project(GEMINI_PROJECT)
                .httpOptions(HttpOptions.builder().
                        apiVersion(String.valueOf(GeminiAPIVersion.V1))
                        .build())
                .build();
        GenerateContentResponse response =
                client.models.generateContent(String.valueOf(GeminiModel.GEMINI_3_FLASH), "Only output: 1.", null);
        Tool googleMaps = Tool.builder().
                googleMaps(GoogleMaps.builder().build())
                .build();

        Schema uuidSchema = Schema.builder()
                .type(Type.Known.STRING)
                .description("The Immutable UUID from the QR Code")
                .build();

        Schema statusSchema = Schema.builder()
                .type(Type.Known.STRING)
                .enum_(Arrays.asList("DELIVERED", "ATTEMPTED", "DAMAGED", "RETURNED"))
                .description("The current status of the package")
                .build();

        Schema confidenceSchema = Schema.builder()
                .type(Type.Known.NUMBER)
                .description("AI confidence score (0.0 to 1.0)")
                .build();
        Schema scanResponseSchema = Schema.builder()
                .type(Type.Known.OBJECT)
                .properties(new HashMap<>() {{
                    // The Holy Grail
                    put("qr_uuid", Schema.builder().type(Type.Known.STRING).nullable(true).description("The UUID if found, else null").build());

                    // The "Why did it fail?" Logic
                    put("scan_status", Schema.builder()
                            .type(Type.Known.STRING)
                            .enum_(Arrays.asList("SUCCESS", "NO_QR_FOUND", "PARTIAL_QR", "UNREADABLE_TEXT"))
                            .build());

                    // The "Fix It" Advice
                    put("quality_issues", Schema.builder()
                            .type(Type.Known.ARRAY)
                            .items(Schema.builder().type(Type.Known.STRING).enum_(Arrays.asList("BLURRY", "LOW_LIGHT", "GLARE", "CUT_OFF", "TORN_LABEL", "HANDWRITING_ILLEGIBLE")).build())
                            .description("List of visual issues detected")
                            .build());

                    // The Payload
                    put("extracted_data", Schema.builder()
                            .type(Type.Known.OBJECT)
                            .properties(new HashMap<>() {{
                                put("recipient", Schema.builder().type(Type.Known.STRING).nullable(true).build());
                                put("address", Schema.builder().type(Type.Known.STRING).nullable(true).build());
                            }})
                            .nullable(true)
                            .build());
                }})
                .required(Arrays.asList("scan_status", "quality_issues")) // Always force it to explain itself
                .build();
        // 2. Build the Properties Map (Because 'putProperties' doesn't exist)
        Map<String, Schema> properties = new HashMap<>();
        properties.put("scan_uuid", uuidSchema);
        properties.put("status", statusSchema);
        properties.put("confidence", confidenceSchema);
        properties.put("scan_response", scanResponseSchema);

        // 3. Define the Root Schema
        Schema scanLogSchema = Schema.builder()
                .type(Type.Known.OBJECT)
                .properties(properties)
                .required(Arrays.asList("scan_uuid", "status")) // Enforce these
                .propertyOrdering(Arrays.asList("scan_uuid", "status", "confidence")) // Help the LLM follow order
                .build();

        // Check nesting
        Schema storedUuid = scanLogSchema.properties().get().get("scan_uuid");

        System.out.println("Schema built successfully: " + scanLogSchema.toString());
        GenerateContentConfig config =
                GenerateContentConfig.builder()
                        .thinkingConfig(ThinkingConfig.builder().thinkingBudget(0))
                        .responseMimeType("application/json")
                        .candidateCount(1)
                        .responseSchema(scanLogSchema)
                        .build();
        

//        GenerateContentConfig config = GenerateContentConfig.builder()
//                                .tools(googleMaps).build();
        System.out.println(response.text());
    }

//    @Async
//    public CompletableFuture<ScanResult> scanImageAsync(MultipartFile file) {
//        try {
//            String base64Image = Base64.getEncoder().encodeToString(file.getBytes());
//
//            GenerationConfig genConfig = GenerationConfig.builder()
//                    .responseMimeType("application/json")
//                    .responseSchema(this.scanSchema)
//                    .temperature(0.0) // ZERO creativity. We want facts.
//                    .build();
//
//            GenerateContentConfig requestConfig = GenerateContentConfig.builder()
//                    .generationConfig(genConfig)
//                    .systemInstruction(this.systemRules) // <--- Logic moved here
//                    .build();
//
//            // The Prompt is now just the trigger
//            Content userPrompt = Content.builder()
//                    .parts(Arrays.asList(
//                            Part.builder().text("Analyze this intake frame.").build(),
//                            Part.builder().inlineData(Blob.builder()
//                                    .mimeType("image/jpeg")
//                                    .data(base64Image)
//                                    .build()).build()
//                    ))
//                    .build();
//
//            // BLOCKING CALL (but inside CompletableFuture, so it's fine)
//            // We do NOT use stream() here because we need the full JSON to parse it.
//            GenerateContentResponse response = geminiClient.models.generateContent(
//                    "gemini-2.0-flash-exp",
//                    userPrompt,
//                    requestConfig
//            );
//
//            // Parse result
//            String json = response.text();
//            ScanResult result = new ObjectMapper().readValue(json, ScanResult.class);
//
//            return CompletableFuture.completedFuture(result);
//
//        } catch (Exception e) {
//            return CompletableFuture.failedFuture(e);
//        }
//    }


    public void generateMapsResponse(String model, String prompt, boolean hasConfig){

    }

}