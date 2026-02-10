package org.tavall.couriers.api.database.config;


public class PostgresConfig {

    public static final String DB_URL = System.getenv("DB_URL");
    public static final String DB_USERNAME = System.getenv("DB_USERNAME");
    public static final String DB_PASSWORD = System.getenv("DB_PASSWORD");
    public static final int DB_POOL_MAX_SIZE = Integer.parseInt(System.getenv("DB_POOL_MAX"));



}