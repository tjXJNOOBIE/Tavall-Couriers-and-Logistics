package org.tavall.couriers.api.web.service.route;

import java.util.List;

public record IntakeRoutePlan(String routeId,
                              List<String> orderedUuids,
                              boolean createNewRoute,
                              String notes) {
}
