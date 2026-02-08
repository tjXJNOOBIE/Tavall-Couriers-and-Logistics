package org.tavall.couriers.api.web.service.route;

import java.util.List;

public record RoutePlan(List<String> orderedUuids, String notes) {
}
