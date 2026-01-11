package org.tavall.gemini.clients.abstracts;


import com.google.genai.Client;
import org.tavall.gemini.enums.GeminiAPIVersion;
import org.tavall.gemini.enums.GeminiModel;

import java.util.ArrayList;
import java.util.List;

public abstract class AbstractGemini3Client extends Client.Builder {

    protected Client client;
    protected final double TEMPERATURE = 0.1;
    protected List<GeminiModel> AVAILABLE_MODELS = new ArrayList<>();
    protected List<GeminiAPIVersion> AVAILABLE_API_VERSIONS = new ArrayList<>();

    public AbstractGemini3Client() {
    }

    public AbstractGemini3Client(Client client) {
        this.client = client;
    }

    public Client getClient() {
        return this.client;
    }

    public abstract void buildClient();

    public boolean hasAvailableModel(GeminiModel geminiModel) {
        return AVAILABLE_MODELS.contains(geminiModel);
    }

    public boolean hasAvailableAPIVersion(GeminiAPIVersion geminiAPIVersion) {
        return AVAILABLE_API_VERSIONS.contains(geminiAPIVersion);
    }
}