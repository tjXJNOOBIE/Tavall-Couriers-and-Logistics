package org.tavall.gemini.clients;


import com.google.genai.Client;
import com.google.genai.types.GenerationConfig;
import com.google.genai.types.HttpOptions;
import org.tavall.gemini.clients.abstracts.AbstractGemini3Client;
import org.tavall.gemini.enums.GeminiAPIVersion;
import org.tavall.gemini.enums.GeminiModel;


public class Gemini3ImageClient extends AbstractGemini3Client {

    // Leave the temperature 0.1 for image resolving
    public Gemini3ImageClient() {
        AVAILABLE_MODELS.add(GeminiModel.GEMINI_3_FLASH);
        AVAILABLE_MODELS.add(GeminiModel.GEMINI_3_PRO);
        AVAILABLE_API_VERSIONS.add(GeminiAPIVersion.V1);
        buildGemini3ImageClient();
    }

    public Gemini3ImageClient(Client client) {
        super(client);
    }

    public void buildGemini3ImageClient() {

        this.client = Client.builder().apiKey
                        (System.getenv("GEMINI_API_KEY"))
                .httpOptions(HttpOptions.builder().
                        apiVersion(String.valueOf(GeminiAPIVersion.V1))
                        .build())
                .build();
    }

    @Override
    public void buildClient() {
        buildGemini3ImageClient();
    }
}