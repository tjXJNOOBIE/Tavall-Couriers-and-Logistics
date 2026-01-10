package org.tavall.gemini.clients;


import com.google.genai.Client;
import com.google.genai.types.HttpOptions;
import org.tavall.gemini.enums.GeminiAPIVersion;
import org.tavall.gemini.enums.GeminiModel;
import org.tavall.gemini.generate.Schemas;

public class Gemini3ImageClient extends Client.Builder{

    private Client client;

    private final double TEMPERATURE = 0.1;
    private List<GeminiModel> AVAILABLE_MODELS = new ArrayList<>();
    private List<Schemas> AVAILABLE_SCHEMAS = new ArrayList<>();
    private List<GeminiAPIVersion> AVAILABLE_API_VERSIONS = new ArrayList<>();
    public Gemini3ImageClient() {

        buildGemini3ImageClient();
    }

    public Gemini3ImageClient(Client client) {
        this.client = client;
    }


    public void buildGemini3ImageClient() {

        this.client = Client.builder().apiKey
                        (System.getenv("GEMINI_API_KEY"))
                .httpOptions(HttpOptions.builder().
                        apiVersion(String.valueOf(GeminiAPIVersion.V1))
                        .build())
                .build();
    }

    public Client getClient() {
        return this.client;
    }


}