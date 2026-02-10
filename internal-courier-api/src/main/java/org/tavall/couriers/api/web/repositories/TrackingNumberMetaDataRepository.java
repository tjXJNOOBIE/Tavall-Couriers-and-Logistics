package org.tavall.couriers.api.web.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.tavall.couriers.api.web.entities.tracking.TrackingNumberMetaDataEntity;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

public interface TrackingNumberMetaDataRepository extends JpaRepository<TrackingNumberMetaDataEntity, String> {
    TrackingNumberMetaDataEntity findByTrackingNumber(String trackingNumber);
    List<TrackingNumberMetaDataEntity> findByTrackingNumberIn(Collection<String> trackingNumbers);
    TrackingNumberMetaDataEntity findByQrUuid(UUID qrUuid);
}
