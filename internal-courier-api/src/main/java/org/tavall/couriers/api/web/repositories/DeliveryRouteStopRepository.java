package org.tavall.couriers.api.web.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.tavall.couriers.api.web.entities.DeliveryRouteStopEntity;

import java.util.List;

public interface DeliveryRouteStopRepository extends JpaRepository<DeliveryRouteStopEntity, String> {
    List<DeliveryRouteStopEntity> findByRouteIdOrderByStopOrderAsc(String routeId);
}
