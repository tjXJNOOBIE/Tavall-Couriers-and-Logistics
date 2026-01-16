package org.tavall.couriers.web.view;

import com.google.genai.Client;
import com.google.genai.types.*;
import org.springframework.stereotype.Service;
import org.tavall.couriers.api.intake.driver.scanner.ai.schemas.ScanResponseSchema;
import org.tavall.couriers.api.web.endpoints.camera.metadata.ScanResponse;
import org.tavall.couriers.api.web.endpoints.camera.state.LiveCameraState;
import org.tavall.gemini.clients.Gemini3ImageClient;
import org.tavall.gemini.enums.GeminiModel;
import tools.jackson.databind.ObjectMapper;

@Service
public class GeminiVisionService {

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final Gemini3ImageClient client;
    private final ScanResponseSchema scanResponseSchema;
    private final ScanResponse scanResponse;
    private Schema schema;

    public GeminiVisionService() {
        this.scanResponse = new ScanResponse(null, null, null, null, null, null, null, "Empty Placeholder Data. Forgot to fill?");
        this.scanResponseSchema = new ScanResponseSchema();
        this.schema = this.scanResponseSchema.getScanResponseSchema();
        this.client = new Gemini3ImageClient(scanResponseSchema.getScanResponseSchema());
    }


    // Input: java.io.File (The physical file on your disk)
    public ScanResponse analyzeFrame(byte[] imageBytes) {

        try {
            // 1. Fail Fast
            if (imageBytes == null || imageBytes.length == 0) {
                return new ScanResponse(null, null, null, null, null, null, null, "Empty Placeholder Data. Forgot to fill?");
            }

            // 2. The Schema-Driven Prompt (Logic Only, No Format Begging)
            String promptText = """
                SYSTEM: You are a Shipping Label OCR Scanner. 
                TASK: Extract data from the shipping label or QR code in this image.
                RULES:
                1. If NO label/QR is legible, return cameraState: "SEARCHING".
                2. If a QR Code contains a UUID, extract it.
                Note: If QR Code does not contain a UUID, return "null" for the "uuid" json output.
                3. Extract Recipient Name, Address, Phone, Tracking Number.
                4. Extract "Deliver By" date as ISO-8601 deadline.
                
                OUTPUT JSON FORMAT (Strict):
                {
                    "uuid": "string or null",
                    "cameraState": "FOUND" or "ERROR",
                    "trackingNumber": "string or null",
                    "name": "string or null",
                    "address": "string or null",
                    "phoneNumber": "string or null",
                    "deadline": "ISO-8601 string or null",
                    "notes": "string"
                }
                """;

            // 3. Construct Content
            Content content = Content.fromParts(
                    Part.fromText(promptText),
                    Part.fromBytes(imageBytes, "image/png"));

            // 4. Config with STRICT SCHEMA

            GenerateContentConfig config = client.getGenerationConfig();
            // 5. Execute
            GenerateContentResponse response = client.getClient().models.generateContent(
                    String.valueOf(GeminiModel.GEMINI_3_FLASH), content, config);

            // 6. Clean & Parse
            String jsonText = response.text();
            // Schema Mode usually returns pure JSON, but we keep the cleaner just in case
            if (jsonText.contains("```json")) {
                jsonText = jsonText.replace("```json", "").replace("```", "").trim();
            }

            return objectMapper.readValue(jsonText, ScanResponse.class);

        } catch (Exception e) {
            System.err.println("Gemini Vision Error: " + e.getMessage());
            return new ScanResponse(null, LiveCameraState.ERROR, null, null, null, null, null, e.getMessage());
        }
    }
}