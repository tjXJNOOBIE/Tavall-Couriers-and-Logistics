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

@Component
public class RouteCacheService extends AbstractCache<RouteCacheService, DeliveryRouteEntity> {
    private ICacheKey<RouteCacheService> cacheKey;
    private ICacheValue<?> cacheValue;

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
            this.cacheKey = (ICacheKey<RouteCacheService>) createKey(
                    this,
                    CacheType.MEMORY,
                    CacheDomain.ROUTES,
                    CacheSource.ROUTE_PLANNER,
                    CacheVersion.V1_0
            );

            this.cacheValue = createValue(route);
            CacheMap.getCacheMap().add(cacheKey, cacheValue);
            Log.success("Route registered in cache.");
            return;
        }
        Log.error("Error: Cannot register null route.");
    }

    public void removeRoute() {
        if (this.cacheKey != null && CacheMap.getCacheMap().containsKey(this.cacheKey)) {
            CacheMap.getCacheMap().remove(this.cacheKey);
            this.cacheKey = null;
            this.cacheValue = null;
            Log.success("Route removed from cache.");
        } else {
            Log.error("Error: Route cache key not found or already removed.");
        }
    }

    public ICacheKey<RouteCacheService> getRouteCacheKey() {
        return this.cacheKey;
    }
}
