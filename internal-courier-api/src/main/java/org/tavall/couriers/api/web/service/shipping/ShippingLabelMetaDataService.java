package org.tavall.couriers.api.web.service.shipping;

import org.springframework.stereotype.Service;
import org.tavall.couriers.api.delivery.state.DeliveryState;
import org.tavall.couriers.api.delivery.state.cache.DeliveryStateCache;
import org.tavall.couriers.api.qr.cache.QRShippingLabelCache;
import org.tavall.couriers.api.tracking.TrackingNumberManager;
import org.tavall.couriers.api.tracking.metadata.TrackingNumberMetaData;
import org.tavall.couriers.api.web.entities.ShippingLabelMetaDataEntity;
import org.tavall.couriers.api.web.repositories.ShippingLabelMetaDataRepository;
import org.springframework.scheduling.TaskScheduler;

import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.time.Duration;
import java.time.Instant;

@Service
public class ShippingLabelMetaDataService {

    private static final Duration PERSIST_DELAY = Duration.ofSeconds(3);

    private final ShippingLabelMetaDataRepository repository;
    private final QRShippingLabelCache qrShippingLabelCache;
    private final DeliveryStateCache deliveryStateCache;
    private final TaskScheduler taskScheduler;

    public ShippingLabelMetaDataService(ShippingLabelMetaDataRepository repository,
                                        QRShippingLabelCache qrShippingLabelCache,
                                        DeliveryStateCache deliveryStateCache,
                                        TaskScheduler taskScheduler) {
        this.repository = repository;
        this.qrShippingLabelCache = qrShippingLabelCache;
        this.deliveryStateCache = deliveryStateCache;
        this.taskScheduler = taskScheduler;
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

        persistAsync(entity, PERSIST_DELAY);
        return entity;
    }

    public ShippingLabelMetaDataEntity createShipmentWithUuid(ShippingLabelMetaDataEntity request,
                                                              String uuid,
                                                              DeliveryState initialState) {
        Objects.requireNonNull(request, "request");
        if (uuid == null || uuid.isBlank()) {
            throw new IllegalArgumentException("uuid is required");
        }

        ShippingLabelMetaDataEntity existing = repository.findById(uuid).orElse(null);
        if (existing != null) {
            return existing;
        }

        UUID parsedUuid = UUID.fromString(uuid);
        TrackingNumberManager trackingManager = new TrackingNumberManager();
        TrackingNumberMetaData trackingMeta = trackingManager.createTrackingNumber(parsedUuid);

        DeliveryState state = initialState != null
                ? initialState
                : (request.getDeliveryState() != null ? request.getDeliveryState() : DeliveryState.LABEL_CREATED);

        ShippingLabelMetaDataEntity entity = new ShippingLabelMetaDataEntity(
                uuid,
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

        persistAsync(entity, PERSIST_DELAY);
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
        persistAsync(entity, Duration.ZERO);
        return entity;
    }

    private void persistAsync(ShippingLabelMetaDataEntity entity, Duration delay) {
        Duration effectiveDelay = delay == null ? Duration.ZERO : delay;
        taskScheduler.schedule(() -> {
            registerCaches(entity);
            repository.save(entity);
        }, Instant.now().plus(effectiveDelay));
    }

    private void registerCaches(ShippingLabelMetaDataEntity entity) {
        qrShippingLabelCache.registerShippingLabel(entity);
        deliveryStateCache.registerDeliveryState(entity);
    }
}
