package org.tavall.couriers.web.beans;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.tavall.springapi.service.cache.ScanCacheService;

@Configuration
public class ScanCacheServiceBean {
    @Bean
    public ScanCacheService getScanCacheService() {
        return new ScanCacheService();
    }

}