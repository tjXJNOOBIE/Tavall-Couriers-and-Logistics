package org.tavall.couriers.api.utils;


public class StringCaseUtil {


    public static String toCamelCase(String screamingSnakeCase) {
        String[] parts = screamingSnakeCase.toLowerCase().split("_");
        StringBuilder camel = new StringBuilder(parts[0]);
        for (int i = 1; i < parts.length; i++) {
            camel.append(parts[i].substring(0, 1).toUpperCase())
                    .append(parts[i].substring(1));
        }
        return camel.toString();
    }
}