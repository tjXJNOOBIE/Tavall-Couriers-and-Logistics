package org.tavall.gemini;

import com.google.genai.Client;
import com.google.genai.types.GenerateContentConfig;
import com.google.genai.types.GenerateContentResponse;
import com.google.genai.types.HttpOptions;
import com.google.genai.types.Schema;
import com.google.genai.types.ThinkingConfig;
import com.google.genai.types.Type;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.tavall.gemini.enums.GeminiAPIVersion;
import org.tavall.gemini.enums.GeminiModel;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class GeminiAPITest {

    private final ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
    private final PrintStream outputLogger = System.out;
    private String apiKey;

    @BeforeEach
    void setUp() {
        System.setOut(new PrintStream(outputStream));

        // Verify API key is available in environment
        apiKey = "AIzaSyBWEogoQdny4ofEQm1y-6V7EF1P9jPj0c4";
        assertThat(apiKey)
                .as("GEMINI_API_KEY environment variable must be set")
                .isNotNull()
                .isNotEmpty();
    }

    @AfterEach
    void tearDown() {
        System.setOut(outputLogger);
    }

    @Test
    @EnabledIfEnvironmentVariable(named = "GEMINI_API_KEY", matches = ".+")
    void testClientConfiguration() {
        // Test that Client can be built with API key and HttpOptions
        Client client = Client.builder()
                .apiKey(apiKey)
                .httpOptions(HttpOptions.builder()
                        .apiVersion(String.valueOf(GeminiAPIVersion.V1))
                        .build())
                .build();

        assertThat(client).isNotNull();
    }

    @Test
    @EnabledIfEnvironmentVariable(named = "GEMINI_API_KEY", matches = ".+")
    void testGenerateResponseWithSchema() {
        // 1. Define the Child Schemas (The "Leaf" nodes)
        Schema uuidSchema = Schema.builder()
                .type(Type.Known.STRING)
                .maxLength(36L)
                .minimum(36.0)
                .nullable(false)
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

        // 2. Build the Properties Map
        Map<String, Schema> properties = new HashMap<>();
        properties.put("scan_uuid", uuidSchema);
        properties.put("status", statusSchema);
        properties.put("confidence", confidenceSchema);

        // 3. Define the Root Schema
        Schema scanLogSchema = Schema.builder()
                .type(Type.Known.OBJECT)
                .properties(properties)
                .required(Arrays.asList("scan_uuid", "status"))
                .propertyOrdering(Arrays.asList("scan_uuid", "status", "confidence"))
                .build();

        // 4. Verify schema was built correctly
        assertNotNull(scanLogSchema, "Schema should not be null");
        Schema storedUuid = scanLogSchema.properties().get().get("scan_uuid");
        assertNotNull(storedUuid, "UUID schema should be accessible");
        outputLogger.println("Schema built successfully: " + storedUuid);

        // 5. Build GenerateContentConfig with schema
        GenerateContentConfig config = GenerateContentConfig.builder()
                .thinkingConfig(ThinkingConfig.builder().thinkingBudget(0))
                .responseMimeType("application/json")
                .candidateCount(1)
                .responseSchema(scanLogSchema)
                .build();

        // 6. Build client with API key and HttpOptions
        Client client = Client.builder()
                .apiKey(apiKey)
                .httpOptions(HttpOptions.builder()
                        .apiVersion(String.valueOf(GeminiAPIVersion.V1_BETA))
                        .build())
                .build();

        // 7. Make actual API call to Gemini with config
        GenerateContentResponse response = client.models.generateContent(
                String.valueOf(GeminiModel.GEMINI_3_FLASH),
                "Generate a scan log entry for UUID '550e8400-e29b-41d4-a716-446655440000' with status DELIVERED",
                config);

        // 8. Verify response
        assertThat(response).isNotNull();
        assertThat(response.text()).isNotEmpty();

        outputLogger.println( "AI Response: " +response.text());

        // 9. Verify output contains expected fields
        assertThat(response.text())
                .isNotEmpty()
                .contains("scan_uuid")
                .contains("status");
    }
}