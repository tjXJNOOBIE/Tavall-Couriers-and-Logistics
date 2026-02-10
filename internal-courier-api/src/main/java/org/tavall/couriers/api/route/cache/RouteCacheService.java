package org.tavall.couriers.api.route.cache;

import org.springframework.stereotype.Component;
import org.tavall.couriers.api.cache.abstracts.AbstractCache;
import org.tavall.couriers.api.cache.enums.CacheDomain;
import org.tavall.couriers.api.cache.enums.CacheSource;
import org.tavall.couriers.api.cache.enums.CacheType;
import org.tavall.couriers.api.cache.enums.CacheVersion;
import org.tavall.couriers.api.cache.interfaces.ICacheKey;
import org.tavall.couriers.api.cache.interfaces.ICacheValue;
import org.tavall.couriers.api.cache.maps.CacheMap;
import org.tavall.couriers.api.console.Log;
import org.tavall.couriers.api.web.entities.DeliveryRouteEntity;
import org.tavall.couriers.api.web.entities.DeliveryRouteStopEntity;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

@Component
public class RouteCacheService extends AbstractCache<RouteCacheService, DeliveryRouteEntity> {
    private ICacheKey<RouteCacheService> cacheKey;
    private ICacheValue<?> cacheValue;
    private final ConcurrentMap<String, DeliveryRouteEntity> routeIndex = new ConcurrentHashMap<>();
    private final ConcurrentMap<String, List<DeliveryRouteStopEntity>> stopIndex = new ConcurrentHashMap<>();
    private volatile boolean primed;

    @Override
    public CacheType getCacheType() {
        return CacheType.MEMORY;
    }

    @Override
    public CacheDomain getCacheDomain() {
        return CacheDomain.ROUTES;
    }

    @Override
    public CacheSource getSource() {
        return CacheSource.ROUTE_PLANNER;
    }

    @Override
    public CacheVersion getVersion() {
        return CacheVersion.V1_0;
    }

    @SuppressWarnings("unchecked")
    public void registerRoute(DeliveryRouteEntity route) {
        if (route != null) {
            ensureCacheKey();
            routeIndex.put(route.getRouteId(), route);
            this.cacheValue = createValue(route);
            CacheMap.getCacheMap().add(cacheKey, cacheValue);
            primed = true;
            Log.success("Route registered in cache: " + route.getRouteId());
            return;
        }
        Log.error("Error: Cannot register null route.");
    }

    public void primeRoutes(Collection<DeliveryRouteEntity> routes) {
        if (routes == null) {
            primed = true;
            return;
        }
        ensureCacheKey();
        for (DeliveryRouteEntity route : routes) {
            if (route == null || route.getRouteId() == null) {
                continue;
            }
            routeIndex.put(route.getRouteId(), route);
            this.cacheValue = createValue(route);
            CacheMap.getCacheMap().add(cacheKey, cacheValue);
        }
        primed = true;
        Log.success("Route cache primed with " + routes.size() + " routes.");
    }

    public void primeStops(Map<String, List<DeliveryRouteStopEntity>> stopsByRoute) {
        if (stopsByRoute == null) {
            return;
        }
        for (Map.Entry<String, List<DeliveryRouteStopEntity>> entry : stopsByRoute.entrySet()) {
            if (entry.getKey() == null) {
                continue;
            }
            List<DeliveryRouteStopEntity> stops = entry.getValue();
            if (stops == null) {
                continue;
            }
            stopIndex.put(entry.getKey(), new ArrayList<>(stops));
        }
    }

    public DeliveryRouteEntity findRoute(String routeId) {
        if (routeId == null || routeId.isBlank()) {
            return null;
        }
        return routeIndex.get(routeId);
    }

    public List<DeliveryRouteEntity> getAllRoutes() {
        if (routeIndex.isEmpty()) {
            return List.of();
        }
        List<DeliveryRouteEntity> routes = new ArrayList<>(routeIndex.values());
        routes.sort(Comparator.comparing(DeliveryRouteEntity::getCreatedAt).reversed());
        return routes;
    }

    public List<DeliveryRouteStopEntity> getRouteStops(String routeId) {
        if (routeId == null || routeId.isBlank()) {
            return List.of();
        }
        List<DeliveryRouteStopEntity> stops = stopIndex.get(routeId);
        if (stops == null || stops.isEmpty()) {
            return List.of();
        }
        return new ArrayList<>(stops);
    }

    public void registerStops(String routeId, List<DeliveryRouteStopEntity> stops) {
        if (routeId == null || routeId.isBlank() || stops == null) {
            return;
        }
        stopIndex.put(routeId, new ArrayList<>(stops));
    }

    public boolean isPrimed() {
        return primed;
    }

    public void removeRoute(String routeId) {
        if (routeId == null || routeId.isBlank()) {
            return;
        }
        routeIndex.remove(routeId);
        stopIndex.remove(routeId);
        Log.success("Route removed from cache: " + routeId);
    }

    public ICacheKey<RouteCacheService> getRouteCacheKey() {
        return this.cacheKey;
    }

    @SuppressWarnings("unchecked")
    private void ensureCacheKey() {
        if (this.cacheKey == null) {
            this.cacheKey = (ICacheKey<RouteCacheService>) createKey(
                    this,
                    CacheType.MEMORY,
                    CacheDomain.ROUTES,
                    CacheSource.ROUTE_PLANNER,
                    CacheVersion.V1_0
            );
        }
    }
}
