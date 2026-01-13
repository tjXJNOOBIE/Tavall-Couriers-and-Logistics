package org.tavall.gemini.clients.config;


import com.google.genai.types.GenerationConfig;

public class Gemini3ImageClientConfig {

    GenerationConfig config;


    public Gemini3ImageClientConfig() {
//       getConfig().toBuilder().temperature()
    }

    public GenerationConfig getConfig() {
        return config;
    }

}