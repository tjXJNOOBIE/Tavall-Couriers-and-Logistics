package org.tavall.couriers.api.web.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.tavall.couriers.api.web.entities.HqLocationEntity;

public interface HqLocationRepository extends JpaRepository<HqLocationEntity, String> {
    HqLocationEntity findFirstByDefaultLocationTrue();
    HqLocationEntity findFirstByOrderByCreatedAtAsc();
}
