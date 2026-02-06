package org.tavall.couriers.api.database;


import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.tavall.couriers.api.database.config.PostgresConfig;

import java.time.Duration;

public class PostgresConnectionManager {

    public static HikariDataSource createPooledConnection() {
        String password = PostgresConfig.DB_PASSWORD;
        String jdbcUrl = PostgresConfig.DB_URL;
        String username = PostgresConfig.DB_USERNAME;
        int maxPoolSize = PostgresConfig.DB_POOL_MAX_SIZE;
        HikariConfig poolConfig = new HikariConfig();
        poolConfig.setJdbcUrl(jdbcUrl);
        poolConfig.setUsername(username);
        poolConfig.setPassword(password);

        // Pool sizing
        poolConfig.setMaximumPoolSize(maxPoolSize);
        poolConfig.setMinimumIdle(Math.min(2, maxPoolSize));

        // Timeouts (don’t let dead DBs stall threads forever)
        poolConfig.setConnectionTimeout(Duration.ofSeconds(10).toMillis());
        poolConfig.setValidationTimeout(Duration.ofSeconds(3).toMillis());
        poolConfig.setIdleTimeout(Duration.ofMinutes(5).toMillis());
        poolConfig.setMaxLifetime(Duration.ofMinutes(30).toMillis());

        // Misc. Data
        poolConfig.setPoolName("tavall-postgres");
        poolConfig.setAutoCommit(true);

        // Optional: detect leaked connections during dev
        // hc.setLeakDetectionThreshold(Duration.ofSeconds(15).toMillis());
        return new HikariDataSource(poolConfig);
    }
}