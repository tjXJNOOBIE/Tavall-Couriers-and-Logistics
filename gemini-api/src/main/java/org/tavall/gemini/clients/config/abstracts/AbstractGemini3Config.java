package org.tavall.gemini.clients.config.abstracts;


import com.google.genai.types.GenerateContentConfig;
import com.google.genai.types.GenerationConfig;
import com.google.genai.types.MediaResolution;

public class AbstractGemini3Config {


    protected final float TEMPERATURE = 0.1f;

    protected int MAX_OUTPUT_TOKENS = 1000;
    protected int CANDIDATE_COUNT = 1;
    protected MediaResolution.Known MEDIA_RESOLUTION = MediaResolution.Known.MEDIA_RESOLUTION_HIGH;
    protected static GenerateContentConfig config;



    public AbstractGemini3Config() {
        if (config == null) {
            config = GenerateContentConfig.builder()
                    .candidateCount(CANDIDATE_COUNT)
                    .temperature(TEMPERATURE)
                    .maxOutputTokens(MAX_OUTPUT_TOKENS)
                    .mediaResolution(MEDIA_RESOLUTION)
                    .responseMimeType("application/json")
                    .build();
        } else {
            config = config.toBuilder()
                    .candidateCount(CANDIDATE_COUNT)
                    .temperature(TEMPERATURE)
                    .maxOutputTokens(MAX_OUTPUT_TOKENS)
                    .mediaResolution(MEDIA_RESOLUTION)
                    .responseMimeType("application/json")
                    .build();
        }
    }


}