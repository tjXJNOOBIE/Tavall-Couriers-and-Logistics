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

        // Properties-first (application.properties / application.yml / profiles)
        String url = firstNonBlank(
                env.getProperty("spring.datasource.url"),
                env.getProperty("NOVUS_POSTGRES_URL")
        );

        String user = firstNonBlank(
                env.getProperty("spring.datasource.username"),
                env.getProperty("NOVUS_POSTGRES_USER")
        );

        String pass = firstNonBlank(
                env.getProperty("spring.datasource.password"),
                env.getProperty("NOVUS_POSTGRES_PASS")
        );

        if (url == null) {
            throw new IllegalStateException(
                    "Missing DB URL. Set spring.datasource.url or NOVUS_POSTGRES_URL"
            );
        }

        if (user == null) {
            throw new IllegalStateException(
                    "Missing DB USER. Set spring.datasource.username or NOVUS_POSTGRES_USER"
            );
        }

        HikariConfig cfg = new HikariConfig();
        cfg.setJdbcUrl(url);
        cfg.setUsername(user);
        cfg.setPassword(pass);

        return new HikariDataSource(cfg);
    }

    private String firstNonBlank(String... values) {
        for (String v : values) {
            if (v != null && !v.isBlank()) {
                return v;
            }
        }
        return null;
    }
}