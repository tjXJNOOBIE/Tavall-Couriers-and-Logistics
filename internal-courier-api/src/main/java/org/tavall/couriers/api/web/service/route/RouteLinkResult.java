package org.tavall.couriers.api.web.service.route;

import java.util.List;

public record RouteLinkResult(String routeUrl, List<String> orderedStops) {
}
