package org.tavall.couriers.api.web.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.tavall.couriers.api.web.entities.ShippingLabelMetaDataEntity;

import java.util.Collection;
import java.util.List;

public interface ShippingLabelMetaDataRepository extends JpaRepository<ShippingLabelMetaDataEntity, String> {
    ShippingLabelMetaDataEntity findByTrackingNumber(String trackingNumber);
    List<ShippingLabelMetaDataEntity> findByTrackingNumberIn(Collection<String> trackingNumbers);
}