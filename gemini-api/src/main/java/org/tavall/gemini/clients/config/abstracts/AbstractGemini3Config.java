package org.tavall.gemini.clients.config.abstracts;


import com.google.genai.types.GenerateContentConfig;
import com.google.genai.types.GenerationConfig;
import com.google.genai.types.MediaResolution;

public class AbstractGemini3Config {


    protected final float TEMPERATURE = 0.1f;
    protected int MAX_OUTPUT_TOKENS = 1000;
    protected MediaResolution.Known MEDIA_RESOLUTION = MediaResolution.Known.MEDIA_RESOLUTION_HIGH;
    protected static GenerateContentConfig config;

    public AbstractGemini3Config() {

    }

    public AbstractGemini3Config(GenerateContentConfig config) {
        AbstractGemini3Config.config = config;
        config.toBuilder()
                .temperature(TEMPERATURE)
                .maxOutputTokens(MAX_OUTPUT_TOKENS)
                .mediaResolution(MEDIA_RESOLUTION)
                .build();
    }


}