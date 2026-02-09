package org.tavall.couriers.api.web.service.route;

import org.springframework.stereotype.Service;
import org.tavall.couriers.api.concurrent.AsyncTask;
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
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

@Service
public class DeliveryRouteService {

    private static final String STATUS_PLANNED = "PLANNED";
    private static final double DEFAULT_RADIUS_MILES = 50.0;
    private static final int DEFAULT_MAX_STOPS = 30;

    private final DeliveryRouteRepository routeRepository;
    private final DeliveryRouteStopRepository stopRepository;
    private final ShippingLabelMetaDataService shippingService;
    private final RoutePlannerService routePlanner;
    private final RouteCacheService routeCache;
    private final GoogleMapsRouteBuilder routeLinkBuilder = new GoogleMapsRouteBuilder();

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
        return createRoute(labels, null, null, null, null);
    }

    public DeliveryRouteEntity createRouteFromLabels(List<String> labelUuids) {
        return createRouteFromLabels(labelUuids, null, null, null, null);
    }

    public DeliveryRouteEntity createRouteFromLabels(List<String> labelUuids,
                                                     UUID assignedDriver,
                                                     Instant deadline,
                                                     Double radiusMiles,
                                                     Integer maxStops) {
        if (labelUuids == null || labelUuids.isEmpty()) {
            return null;
        }
        List<ShippingLabelMetaDataEntity> labels = new ArrayList<>();
        for (String uuid : labelUuids) {
            if (uuid == null || uuid.isBlank()) {
                continue;
            }
            ShippingLabelMetaDataEntity label = shippingService.findByUuid(uuid);
            if (label != null) {
                labels.add(label);
            }
        }
        return createRoute(labels, assignedDriver, deadline, radiusMiles, maxStops);
    }

    public DeliveryRouteEntity createRoute(List<ShippingLabelMetaDataEntity> labels,
                                           UUID assignedDriver,
                                           Instant deadline,
                                           Double radiusMiles,
                                           Integer maxStops) {
        if (labels == null || labels.isEmpty()) {
            return null;
        }

        double resolvedRadius = radiusMiles != null && radiusMiles > 0 ? radiusMiles : DEFAULT_RADIUS_MILES;
        int resolvedMaxStops = maxStops != null && maxStops > 0 ? maxStops : DEFAULT_MAX_STOPS;

        Map<String, ShippingLabelMetaDataEntity> labelMap = new HashMap<>();
        for (ShippingLabelMetaDataEntity label : labels) {
            if (label != null && label.getUuid() != null) {
                labelMap.put(label.getUuid(), label);
            }
        }

        RoutePlan plan = routePlanner.planRoute(labels, resolvedRadius, resolvedMaxStops);
        List<String> normalized = new ArrayList<>();
        Map<String, Boolean> seen = new HashMap<>();
        if (plan != null && plan.orderedUuids() != null) {
            for (String uuid : plan.orderedUuids()) {
                if (uuid == null || !labelMap.containsKey(uuid) || normalized.size() >= resolvedMaxStops) {
                    continue;
                }
                if (seen.putIfAbsent(uuid, Boolean.TRUE) == null) {
                    normalized.add(uuid);
                }
            }
        }
        for (ShippingLabelMetaDataEntity label : labels) {
            if (label == null || label.getUuid() == null || normalized.size() >= resolvedMaxStops) {
                continue;
            }
            String uuid = label.getUuid();
            if (seen.putIfAbsent(uuid, Boolean.TRUE) == null) {
                normalized.add(uuid);
            }
        }

        String routeId = generateRouteId();
        Instant now = Instant.now();
        Instant resolvedDeadline = resolveDeadline(labels, deadline);
        String routeLink = resolveRouteLink(normalized, labelMap);
        DeliveryRouteEntity route = new DeliveryRouteEntity(
                routeId,
                STATUS_PLANNED,
                normalized.size(),
                now,
                now,
                plan != null ? plan.notes() : null,
                assignedDriver,
                resolvedDeadline,
                routeLink
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

    public DeliveryRouteEntity addStops(String routeId, List<String> labelUuids) {
        if (routeId == null || routeId.isBlank() || labelUuids == null || labelUuids.isEmpty()) {
            return null;
        }
        DeliveryRouteEntity route = findRoute(routeId);
        if (route == null) {
            return null;
        }

        List<String> normalized = new ArrayList<>();
        Map<String, ShippingLabelMetaDataEntity> metadataLookup = new HashMap<>();
        for (String uuid : labelUuids) {
            if (uuid == null) {
                continue;
            }
            String trimmed = uuid.trim();
            if (trimmed.isBlank()) {
                continue;
            }
            ShippingLabelMetaDataEntity label = shippingService.findByUuid(trimmed);
            if (label == null) {
                continue;
            }
            normalized.add(trimmed);
            metadataLookup.putIfAbsent(trimmed, label);
        }

        if (normalized.isEmpty()) {
            return null;
        }

        Instant now = Instant.now();
        int existingCount = getRouteStops(routeId).size();
        List<DeliveryRouteStopEntity> newStops = buildStops(routeId, normalized, now, existingCount + 1);
        stopRepository.saveAll(newStops);

        List<DeliveryRouteStopEntity> allStops = getRouteStops(routeId);
        routeCache.registerStops(routeId, allStops);

        List<String> allUuids = new ArrayList<>();
        for (DeliveryRouteStopEntity stop : allStops) {
            if (stop == null) {
                continue;
            }
            String stopUuid = stop.getLabelUuid();
            if (stopUuid == null) {
                continue;
            }
            allUuids.add(stopUuid);
            metadataLookup.computeIfAbsent(stopUuid, shippingService::findByUuid);
        }

        route.setLabelCount(allStops.size());
        route.setUpdatedAt(now);
        String existingLink = route.getRouteLink();
        String newRouteLink = resolveRouteLink(allUuids, metadataLookup);
        if (newRouteLink != null && !newRouteLink.isBlank()) {
            route.setRouteLink(newRouteLink);
            Log.info("Route " + routeId + " link refreshed.");
        } else {
            route.setRouteLink(existingLink);
            Log.warn("Route " + routeId + " link refresh failed; keeping existing link.");
        }
        DeliveryRouteEntity saved = routeRepository.save(route);
        routeCache.registerRoute(saved);
        Log.info("Route " + routeId + " added stops: " + normalized.size() + " (total " + saved.getLabelCount() + ")");
        return saved;
    }

    public DeliveryRouteEntity assignDriver(String routeId, UUID driverId) {
        DeliveryRouteEntity route = findRoute(routeId);
        if (route == null) {
            return null;
        }
        route.setAssignedDrivers(driverId);
        route.setUpdatedAt(Instant.now());
        DeliveryRouteEntity updated = routeRepository.save(route);
        routeCache.registerRoute(updated);
        Log.info("Driver assigned to route " + routeId + ": " + (driverId != null ? driverId : "none"));
        return updated;
    }

    public void addStopAsync(String routeId, String labelUuid) {
        if (routeId == null || routeId.isBlank() || labelUuid == null || labelUuid.isBlank()) {
            return;
        }
        CompletableFuture<Void> future = AsyncTask.runFuture(() -> {
            addStops(routeId, List.of(labelUuid));
            return null;
        }, AsyncTask.ScopeOptions.defaults().withName("route-stop-add"));
        future.exceptionally(ex -> {
            Log.error("Failed to add stop async for route " + routeId + ": " + ex.getMessage());
            Log.exception(ex);
            return null;
        });
        Log.info("Scheduled async stop add for route " + routeId);
    }

    public double getDefaultRadiusMiles() {
        return DEFAULT_RADIUS_MILES;
    }

    public int getDefaultMaxStops() {
        return DEFAULT_MAX_STOPS;
    }

    private List<DeliveryRouteStopEntity> buildStops(String routeId, List<String> uuids, Instant now) {
        return buildStops(routeId, uuids, now, 1);
    }

    private List<DeliveryRouteStopEntity> buildStops(String routeId,
                                                    List<String> uuids,
                                                    Instant now,
                                                    int startingOrder) {
        List<DeliveryRouteStopEntity> stops = new ArrayList<>();
        int order = Math.max(1, startingOrder);
        for (String uuid : uuids) {
            if (uuid == null || uuid.isBlank()) {
                continue;
            }
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

    private Instant resolveDeadline(List<ShippingLabelMetaDataEntity> labels, Instant override) {
        if (override != null) {
            return override;
        }
        Instant earliest = null;
        if (labels == null) {
            return null;
        }
        for (ShippingLabelMetaDataEntity label : labels) {
            if (label == null || label.getDeliverBy() == null) {
                continue;
            }
            if (earliest == null || label.getDeliverBy().isBefore(earliest)) {
                earliest = label.getDeliverBy();
            }
        }
        return earliest;
    }

    private String resolveRouteLink(List<String> uuids, Map<String, ShippingLabelMetaDataEntity> labelMap) {
        if (uuids == null || uuids.isEmpty() || labelMap == null || labelMap.isEmpty()) {
            return null;
        }
        List<String> addresses = new ArrayList<>();
        for (String uuid : uuids) {
            ShippingLabelMetaDataEntity label = labelMap.get(uuid);
            if (label == null) {
                continue;
            }
            String formatted = formatStopAddress(label);
            if (!formatted.isBlank()) {
                addresses.add(formatted);
            }
        }
        if (addresses.isEmpty()) {
            return null;
        }
        RouteLinkResult result = routeLinkBuilder.buildRouteLink(addresses);
        return result != null ? result.routeUrl() : null;
    }

    private String formatStopAddress(ShippingLabelMetaDataEntity label) {
        if (label == null) {
            return "";
        }
        StringBuilder builder = new StringBuilder();
        appendAddressPart(builder, label.getAddress());
        appendAddressPart(builder, label.getCity());
        appendAddressPart(builder, label.getState());
        appendZipCode(builder, label.getZipCode());
        return builder.toString().trim();
    }

    private void appendAddressPart(StringBuilder builder, String part) {
        if (part == null || part.isBlank()) {
            return;
        }
        if (builder.length() > 0) {
            builder.append(", ");
        }
        builder.append(part.trim());
    }

    private void appendZipCode(StringBuilder builder, String zip) {
        if (zip == null || zip.isBlank()) {
            return;
        }
        if (builder.length() > 0) {
            builder.append(" ");
        }
        builder.append(zip.trim());
    }
}



