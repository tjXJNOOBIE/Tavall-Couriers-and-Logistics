package org.tavall.couriers.api.web.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.tavall.couriers.api.shipping.database.entities.ShippingLabelMetaDataEntity;

public interface ShippingLabelMetaDataRepository extends JpaRepository<ShippingLabelMetaDataEntity, String> {
}