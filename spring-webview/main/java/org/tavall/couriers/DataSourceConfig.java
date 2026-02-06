package org.tavall.couriers;


import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;

@Configuration
public class DataSourceConfig {



    @Bean(destroyMethod = "close")
    public HikariDataSource dataSource(Environment env) { 
        String url = env.getProperty("NOVUS_POSTGRES_URL");
        String user = env.getProperty("NOVUS_POSTGRES_USER"); 
        String pass = env.getProperty("NOVUS_POSTGRES_PASS"); 

        // Optional fallback to Spring standard keys if you want both supported 
        if (url == null || url.isBlank()) url = env.getProperty("spring.datasource.url");
        if (user == null || user.isBlank()) user = env.getProperty("spring.datasource.username"); 
        if (pass == null) pass = env.getProperty("spring.datasource.password"); 

        if (url == null || url.isBlank()) { 
            throw new IllegalStateException("Missing DB URL. Set NOVUS_POSTGRES_URL or spring.datasource.url"); 
        } 
        if (user == null || user.isBlank()) { 
            throw new IllegalStateException("Missing DB USER. Set NOVUS_POSTGRES_USER or spring.datasource.username"); 
        }
        HikariConfig cfg = new HikariConfig();

        cfg.setJdbcUrl(url);
        cfg.setUsername(user);
        cfg.setPassword(pass);
        return new HikariDataSource(cfg);
    }
    
}