package org.tavall.couriers.web.beans;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.tavall.gemini.clients.Gemini3ImageClient;

@Configuration
public class GeminiImageClientBean {

    @Bean
    public Gemini3ImageClient gemini3ImageClient() {
        return new Gemini3ImageClient();
    }
}