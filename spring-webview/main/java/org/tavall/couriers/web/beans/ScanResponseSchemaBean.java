package org.tavall.couriers.web.beans;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.tavall.couriers.api.intake.driver.scanner.ai.schemas.ScanResponseSchema;

@Configuration
public class ScanResponseSchemaBean {

    @Bean
    public ScanResponseSchema getScanResponseSchema() {
        return new ScanResponseSchema();
    }
}