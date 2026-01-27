package org.tavall.couriers.web.beans;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.tavall.couriers.api.cache.maps.CacheMap;

@Configuration
public class CacheMapBean {


    @Bean
    public CacheMap getCacheMap() {
        return CacheMap.INSTANCE;
    }
}