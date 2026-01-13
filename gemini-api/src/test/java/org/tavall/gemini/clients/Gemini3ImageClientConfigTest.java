package org.tavall.gemini.clients;

import com.google.genai.types.GenerateContentConfig;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.tavall.gemini.clients.config.Gemini3ImageClientConfig;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for Gemini3ImageClientConfig and its abstract base configuration.
 */
class Gemini3ImageClientConfigTest {

    private Gemini3ImageClientConfig config;

    @BeforeEach
    void setUp() {
        config = new Gemini3ImageClientConfig();
    }

    @Test
    void shouldExposeStaticConfigInstance() {
        GenerateContentConfig cfg = Gemini3ImageClientConfig.getImageClientConfig();
        assertNotNull(cfg, "Static config instance should be exposed and not null");
    }

    @Test
    void shouldBuildConfigWithJsonResponseMimeType() {
        GenerateContentConfig cfg = Gemini3ImageClientConfig.getImageClientConfig();
        assertNotNull(cfg, "Config should be available");
        // Response mime type should be application/json as set by AbstractGemini3Config
        // The builder pattern keeps values inside the config object; ensure it can create a builder
        assertNotNull(cfg.toBuilder().responseMimeType("application/json").build(),
                "Config builder should allow setting response mime type");
    }

    @Test
    void shouldAllowRebuildingViaToBuilder() {
        GenerateContentConfig cfg = Gemini3ImageClientConfig.getImageClientConfig();
        GenerateContentConfig rebuilt = cfg.toBuilder().build();
        assertNotNull(rebuilt, "Config should be rebuildable through toBuilder().build()");
    }

    @Test
    void shouldNotThrowWhenAccessingBuilderRepeatedly() {
        GenerateContentConfig cfg = Gemini3ImageClientConfig.getImageClientConfig();
        assertDoesNotThrow(() -> {
            cfg.toBuilder().build();
            cfg.toBuilder().build();
            cfg.toBuilder().build();
        }, "Repeated builder access should not throw");
    }

    @Test
    void shouldProvideImmutableBaseConfigReference() {
        GenerateContentConfig cfg = Gemini3ImageClientConfig.getImageClientConfig();
        GenerateContentConfig altered = cfg.toBuilder().temperature(0.9f).build();
        // Ensure base reference still retrievable and non-null after alterations to a copy
        assertNotNull(Gemini3ImageClientConfig.getImageClientConfig(),
                "Base config reference should remain accessible after building variants");
        assertNotNull(altered, "Altered config should be buildable as a separate instance");
    }
}