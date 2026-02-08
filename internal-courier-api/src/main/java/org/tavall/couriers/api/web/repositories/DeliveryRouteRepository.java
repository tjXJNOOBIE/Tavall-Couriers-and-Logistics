package org.tavall.couriers.api.web.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.tavall.couriers.api.web.entities.DeliveryRouteEntity;

import java.util.List;

public interface DeliveryRouteRepository extends JpaRepository<DeliveryRouteEntity, String> {
    List<DeliveryRouteEntity> findAllByOrderByCreatedAtDesc();
}
