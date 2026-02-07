package org.tavall.couriers.api.web.service.shipping;

import org.springframework.stereotype.Service;
import org.tavall.couriers.api.concurrent.AsyncTask;
import org.tavall.couriers.api.delivery.state.DeliveryState;
import org.tavall.couriers.api.delivery.state.cache.DeliveryStateCache;
import org.tavall.couriers.api.qr.cache.QRShippingLabelCache;
import org.tavall.couriers.api.tracking.TrackingNumberManager;
import org.tavall.couriers.api.tracking.metadata.TrackingNumberMetaData;
import org.tavall.couriers.api.web.entities.ShippingLabelMetaDataEntity;
import org.tavall.couriers.api.web.repositories.ShippingLabelMetaDataRepository;

import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

@Service
public class ShippingLabelMetaDataService {

    private final ShippingLabelMetaDataRepository repository;

    public ShippingLabelMetaDataService(ShippingLabelMetaDataRepository repository) {
        this.repository = repository;
    }

    public List<ShippingLabelMetaDataEntity> getAllShipmentLabels() {
        return repository.findAll();
    }

    public ShippingLabelMetaDataEntity findByTrackingNumber(String trackingNumber) {
        if (trackingNumber == null || trackingNumber.isBlank()) {
            return null;
        }
        return repository.findByTrackingNumber(trackingNumber);
    }

    public List<ShippingLabelMetaDataEntity> findByTrackingNumbers(Collection<String> trackingNumbers) {
        if (trackingNumbers == null || trackingNumbers.isEmpty()) {
            return List.of();
        }
        return repository.findByTrackingNumberIn(trackingNumbers);
    }

    public ShippingLabelMetaDataEntity findByUuid(String uuid) {
        if (uuid == null || uuid.isBlank()) {
            return null;
        }
        return repository.findById(uuid).orElse(null);
    }

    public ShippingLabelMetaDataEntity createShipment(ShippingLabelMetaDataEntity request, DeliveryState initialState) {
        Objects.requireNonNull(request, "request");

        UUID uuid = UUID.randomUUID();
        TrackingNumberManager trackingManager = new TrackingNumberManager();
        TrackingNumberMetaData trackingMeta = trackingManager.createTrackingNumber(uuid);

        DeliveryState state = initialState != null
                ? initialState
                : (request.getDeliveryState() != null ? request.getDeliveryState() : DeliveryState.LABEL_CREATED);

        ShippingLabelMetaDataEntity entity = new ShippingLabelMetaDataEntity(
                uuid.toString(),
                trackingMeta.trackingNumber(),
                request.getRecipientName(),
                request.getPhoneNumber(),
                request.getAddress(),
                request.getCity(),
                request.getState(),
                request.getZipCode(),
                request.getCountry(),
                request.isPriority(),
                request.getDeliverBy(),
                state
        );

        persistAsync(entity);
        return entity;
    }

    public ShippingLabelMetaDataEntity createShipment(ShippingLabelMetaDataEntity request) {
        return createShipment(request, DeliveryState.LABEL_CREATED);
    }

    public ShippingLabelMetaDataEntity updateDeliveryState(String uuid, DeliveryState targetState) {
        Objects.requireNonNull(uuid, "uuid");
        Objects.requireNonNull(targetState, "targetState");

        ShippingLabelMetaDataEntity entity = repository.findById(uuid).orElse(null);
        if (entity == null) {
            return null;
        }

        entity.setDeliveryState(targetState);
        persistAsync(entity);
        return entity;
    }

    private void persistAsync(ShippingLabelMetaDataEntity entity) {
        AsyncTask.runFuture(() -> {
            registerCaches(entity);
            return repository.save(entity);
        });
    }

    private void registerCaches(ShippingLabelMetaDataEntity entity) {
        QRShippingLabelCache.INSTANCE.registerShippingLabel(entity);
        new DeliveryStateCache().registerDeliveryState(entity);
    }
}
