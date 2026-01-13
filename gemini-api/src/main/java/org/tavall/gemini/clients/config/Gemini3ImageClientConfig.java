package org.tavall.gemini.clients.config;


import com.google.genai.types.GenerateContentConfig;
import com.google.genai.types.GenerationConfig;
import org.tavall.gemini.clients.config.abstracts.AbstractGemini3Config;

public class Gemini3ImageClientConfig extends AbstractGemini3Config {



    public Gemini3ImageClientConfig() {
    super();
    }

    public static GenerateContentConfig getConfig() {
        return config;
    }

}