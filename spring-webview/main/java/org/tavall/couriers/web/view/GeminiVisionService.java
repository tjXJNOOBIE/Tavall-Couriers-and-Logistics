package org.tavall.couriers.web.view;

import com.google.genai.Client;
import com.google.genai.types.*;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.tavall.couriers.api.web.endpoints.camera.metadata.ScanResponse;
import org.tavall.gemini.clients.Gemini3ImageClient;
import org.tavall.gemini.clients.config.Gemini3ImageClientConfig;
import org.tavall.gemini.enums.GeminiModel;
import tools.jackson.databind.ObjectMapper;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Base64;
import java.util.List;

@Service
public class GeminiVisionService {
    private final ObjectMapper objectMapper;
    private final Client client;
    public GeminiVisionService(Gemini3ImageClient clientWrapper, ObjectMapper objectMapper) {
        this.client = clientWrapper.getClient();
        this.objectMapper = objectMapper;
    }

    // Input: java.io.File (The physical file on your disk)
    public ScanResponse analyzeFrame(byte[] imageBytes) {
        try {
            // 1. Fail Fast
            if (imageBytes == null || imageBytes.length == 0) {
                return new ScanResponse(null, "ERROR", null, null, null, null, "Empty Frame Data");
            }

//            File googleFile = client.files.upload(
//                    diskFile.getAbsolutePath(),
//                    UploadFileConfig.builder().mimeType("image/png").build()
//            );

            // 3. The Prompt
            String promptText = """
                SYSTEM: You are a Shipping Label OCR Scanner. 
                TASK: Extract data from the shipping label or QR code in this image.
                RULES:
                1. If NO label/QR is legible, return status: "SEARCHING".
                2. If a QR Code contains a UUID, extract it.
                3. Extract Recipient Name, Address, Phone, Tracking Number.
                
                OUTPUT JSON FORMAT (Strict):
                {
                    "uuid": "string or null",
                    "status": "FOUND" or "SEARCHING",
                    "trackingNumber": "string or null",
                    "name": "string or null",
                    "address": "string or null",
                    "phoneNumber": "string or null",
                    "notes": "string"
                }
                """;

            // 4. Create Content using the Google File URI
            Content content = Content.fromParts(
                    Part.fromText(promptText),
                    Part.fromBytes(imageBytes,"image/png"));


            // 5. Config
//            GenerateContentConfig config = Gemini3ImageClientConfig.getImageClientConfig().toBuilder().build();

            // 6. Execute
            GenerateContentResponse response = client.models.generateContent(
                    String.valueOf(GeminiModel.GEMINI_2_5_FLASH),
                    content,null);

            // 7. Parse
            String jsonText = response.text();
            if (jsonText.contains("```json")) {
                jsonText = jsonText.replace("```json", "").replace("```", "").trim();
            }

            return objectMapper.readValue(jsonText, ScanResponse.class);

        } catch (Exception e) {
            System.err.println("Gemini Vision Error: " + e.getMessage());
            e.printStackTrace();
            return new ScanResponse(null, "ERROR", null, null, null, null, e.getMessage());
        }
    }
}