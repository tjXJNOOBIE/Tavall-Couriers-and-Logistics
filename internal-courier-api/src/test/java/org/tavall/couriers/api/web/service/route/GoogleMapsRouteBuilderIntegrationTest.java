package org.tavall.couriers.api.web.service.route;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Assumptions;
import org.tavall.couriers.api.console.Log;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GoogleMapsRouteBuilderIntegrationTest {

    @Test
    void buildRouteLinkTest() {
        Log.info("Starting test: buildRouteLinkTest");
        Assumptions.assumeTrue(hasApiKey(), "GEMINI_API_KEY missing; skipping live route build.");

        GoogleMapsRouteBuilderDelegate delegate = new GoogleMapsRouteBuilderDelegate();
        List<String> stops = List.of(
                "1600 Amphitheatre Parkway, Mountain View, CA",
                "1 Infinite Loop, Cupertino, CA",
                "500 Terry A Francois Blvd, San Francisco, CA"
        );

        RouteLinkResult result = delegate.buildRouteLink(stops);
        assertNotNull(result);
        assertNotNull(result.routeUrl());
        assertTrue(result.routeUrl().contains("google.com/maps/dir"));

        Log.success("Route URL: " + result.routeUrl());
    }

    private boolean hasApiKey() {
        String apiKey = System.getenv("GEMINI_API_KEY");
        return apiKey != null && !apiKey.isBlank();
    }

}
