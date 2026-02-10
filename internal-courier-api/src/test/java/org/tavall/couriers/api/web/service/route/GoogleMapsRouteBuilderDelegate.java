package org.tavall.couriers.api.web.service.route;

import java.util.List;

class GoogleMapsRouteBuilderDelegate {

    private final GoogleMapsRouteBuilder builder = new GoogleMapsRouteBuilder();

    public RouteLinkResult buildRouteLink(List<String> stops) {
        return builder.buildRouteLink(stops);
    }
}
