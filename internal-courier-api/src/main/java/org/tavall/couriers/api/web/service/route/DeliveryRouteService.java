package org.tavall.couriers.api.web.service.route;

import org.springframework.stereotype.Service;
import org.tavall.couriers.api.console.Log;
import org.tavall.couriers.api.route.cache.RouteCacheService;
import org.tavall.couriers.api.web.entities.DeliveryRouteEntity;
import org.tavall.couriers.api.web.entities.DeliveryRouteStopEntity;
import org.tavall.couriers.api.web.entities.ShippingLabelMetaDataEntity;
import org.tavall.couriers.api.web.repositories.DeliveryRouteRepository;
import org.tavall.couriers.api.web.repositories.DeliveryRouteStopRepository;
import org.tavall.couriers.api.web.service.shipping.ShippingLabelMetaDataService;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@Service
public class DeliveryRouteService {

    private static final String STATUS_PLANNED = "PLANNED";

    private final DeliveryRouteRepository routeRepository;
    private final DeliveryRouteStopRepository stopRepository;
    private final ShippingLabelMetaDataService shippingService;
    private final RoutePlannerService routePlanner;
    private final RouteCacheService routeCache;

    public DeliveryRouteService(DeliveryRouteRepository routeRepository,
                                DeliveryRouteStopRepository stopRepository,
                                ShippingLabelMetaDataService shippingService,
                                RoutePlannerService routePlanner,
                                RouteCacheService routeCache) {
        this.routeRepository = routeRepository;
        this.stopRepository = stopRepository;
        this.shippingService = shippingService;
        this.routePlanner = routePlanner;
        this.routeCache = routeCache;
    }

    public List<DeliveryRouteEntity> getAllRoutes() {
        return routeRepository.findAllByOrderByCreatedAtDesc();
    }

    public DeliveryRouteEntity findRoute(String routeId) {
        if (routeId == null || routeId.isBlank()) {
            return null;
        }
        return routeRepository.findById(routeId).orElse(null);
    }

    public List<DeliveryRouteStopEntity> getRouteStops(String routeId) {
        if (routeId == null || routeId.isBlank()) {
            return List.of();
        }
        return stopRepository.findByRouteIdOrderByStopOrderAsc(routeId);
    }

    public DeliveryRouteEntity createRouteFromAllLabels() {
        List<ShippingLabelMetaDataEntity> labels = shippingService.getAllShipmentLabels();
        return createRoute(labels);
    }

    public DeliveryRouteEntity createRoute(List<ShippingLabelMetaDataEntity> labels) {
        if (labels == null || labels.isEmpty()) {
            return null;
        }

        Map<String, ShippingLabelMetaDataEntity> labelMap = new HashMap<>();
        for (ShippingLabelMetaDataEntity label : labels) {
            if (label != null && label.getUuid() != null) {
                labelMap.put(label.getUuid(), label);
            }
        }

        RoutePlan plan = routePlanner.planRoute(labels);
        List<String> orderedUuids = new ArrayList<>();
        if (plan != null && plan.orderedUuids() != null) {
            orderedUuids.addAll(plan.orderedUuids());
        }

        Set<String> seen = new HashSet<>();
        List<String> normalized = new ArrayList<>();
        for (String uuid : orderedUuids) {
            if (uuid == null || !labelMap.containsKey(uuid) || seen.contains(uuid)) {
                continue;
            }
            normalized.add(uuid);
            seen.add(uuid);
        }
        for (ShippingLabelMetaDataEntity label : labels) {
            if (label != null && label.getUuid() != null && !seen.contains(label.getUuid())) {
                normalized.add(label.getUuid());
                seen.add(label.getUuid());
            }
        }

        String routeId = generateRouteId();
        Instant now = Instant.now();
        DeliveryRouteEntity route = new DeliveryRouteEntity(
                routeId,
                STATUS_PLANNED,
                normalized.size(),
                now,
                now,
                plan != null ? plan.notes() : null
        );

        routeRepository.save(route);
        stopRepository.saveAll(buildStops(routeId, normalized, now));
        routeCache.registerRoute(route);
        Log.success("Route created: " + routeId + " (" + normalized.size() + " stops)");
        return route;
    }

    public boolean deleteRoute(String routeId) {
        if (routeId == null || routeId.isBlank()) {
            return false;
        }
        DeliveryRouteEntity route = findRoute(routeId);
        if (route == null) {
            return false;
        }
        routeRepository.deleteById(routeId);
        Log.info("Route deleted: " + routeId);
        return true;
    }

    public DeliveryRouteEntity updateRoute(String routeId, String status, String notes) {
        DeliveryRouteEntity route = findRoute(routeId);
        if (route == null) {
            return null;
        }
        if (status != null && !status.isBlank()) {
            route.setStatus(status.trim().toUpperCase());
        }
        if (notes != null) {
            route.setNotes(notes.trim());
        }
        route.setUpdatedAt(Instant.now());
        return routeRepository.save(route);
    }

    private List<DeliveryRouteStopEntity> buildStops(String routeId, List<String> uuids, Instant now) {
        List<DeliveryRouteStopEntity> stops = new ArrayList<>();
        int order = 1;
        for (String uuid : uuids) {
            stops.add(new DeliveryRouteStopEntity(
                    UUID.randomUUID().toString(),
                    routeId,
                    uuid,
                    order,
                    now
            ));
            order++;
        }
        return stops;
    }

    private String generateRouteId() {
        return "RTE-" + UUID.randomUUID();
    }
}
