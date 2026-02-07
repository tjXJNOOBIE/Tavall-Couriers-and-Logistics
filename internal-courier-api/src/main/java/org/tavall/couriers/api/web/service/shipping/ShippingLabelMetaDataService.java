package org.tavall.couriers.api.web.service.shipping;

import org.springframework.stereotype.Service;
import org.tavall.couriers.api.shipping.database.entities.ShippingLabelMetaDataEntity;
import org.tavall.couriers.api.web.repositories.ShippingLabelMetaDataRepository;

import java.util.List;

@Service
public class ShippingLabelMetaDataService {

    private final ShippingLabelMetaDataRepository repository;

    public ShippingLabelMetaDataService(ShippingLabelMetaDataRepository repository) {
        this.repository = repository;
    }

    public List<ShippingLabelMetaDataEntity> getAllShipmentLabels() {
        return repository.findAll();
    }
}