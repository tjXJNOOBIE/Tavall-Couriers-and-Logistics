package org.tavall.couriers.api.web.service.shipping;

import jakarta.annotation.PostConstruct;
import org.springframework.stereotype.Service;
import org.tavall.couriers.api.concurrent.AsyncTask;
import org.tavall.couriers.api.console.Log;
import org.tavall.couriers.api.delivery.state.DeliveryState;
import org.tavall.couriers.api.delivery.state.cache.DeliveryStateCache;
import org.tavall.couriers.api.qr.cache.QRShippingLabelCache;
import org.tavall.couriers.api.web.entities.ShippingLabelMetaDataEntity;
import org.tavall.couriers.api.web.entities.tracking.TrackingNumberMetaDataEntity;
import org.tavall.couriers.api.web.repositories.ShippingLabelMetaDataRepository;
import org.tavall.couriers.api.web.service.tracking.TrackingNumberMetaDataService;

import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.UUID;
import java.time.Duration;
import java.util.concurrent.atomic.AtomicBoolean;

@Service
public class ShippingLabelMetaDataService {

    private static final Duration PERSIST_DELAY = Duration.ofSeconds(3);

    private final ShippingLabelMetaDataRepository repository;
    private final QRShippingLabelCache qrShippingLabelCache;
    private final DeliveryStateCache deliveryStateCache;
    private final TrackingNumberMetaDataService trackingService;
    private final AtomicBoolean priming = new AtomicBoolean(false);

    public ShippingLabelMetaDataService(ShippingLabelMetaDataRepository repository,
                                        QRShippingLabelCache qrShippingLabelCache,
                                        DeliveryStateCache deliveryStateCache,
                                        TrackingNumberMetaDataService trackingService) {
        this.repository = repository;
        this.qrShippingLabelCache = qrShippingLabelCache;
        this.deliveryStateCache = deliveryStateCache;
        this.trackingService = trackingService;
    }

    @PostConstruct
    public void warmCaches() {
        primeCachesFromDatabase();
    }

    public List<ShippingLabelMetaDataEntity> getAllShipmentLabels() {
        List<ShippingLabelMetaDataEntity> cached = qrShippingLabelCache.getAllLabels();
        if (!cached.isEmpty() || qrShippingLabelCache.isPrimed()) {
            return cached;
        }
        primeCachesAsync();
        return cached;
    }

    public ShippingLabelMetaDataEntity findByTrackingNumber(String trackingNumber) {
        if (trackingNumber == null || trackingNumber.isBlank()) {
            return null;
        }
        String normalized = trackingNumber.trim().toUpperCase(Locale.ROOT);
        ShippingLabelMetaDataEntity cached = qrShippingLabelCache.findByTrackingNumber(normalized);
        if (cached != null || qrShippingLabelCache.isPrimed()) {
            return cached;
        }
        primeCachesAsync();
        return cached;
    }

    public List<ShippingLabelMetaDataEntity> findByTrackingNumbers(Collection<String> trackingNumbers) {
        if (trackingNumbers == null || trackingNumbers.isEmpty()) {
            return List.of();
        }
        List<ShippingLabelMetaDataEntity> cached = qrShippingLabelCache.findByTrackingNumbers(trackingNumbers);
        if (!cached.isEmpty() || qrShippingLabelCache.isPrimed()) {
            return cached;
        }
        primeCachesAsync();
        return cached;
    }

    public ShippingLabelMetaDataEntity findByUuid(String uuid) {
        if (uuid == null || uuid.isBlank()) {
            return null;
        }
        ShippingLabelMetaDataEntity cached = qrShippingLabelCache.findByUuid(uuid);
        if (cached != null || qrShippingLabelCache.isPrimed()) {
            return cached;
        }
        primeCachesAsync();
        return cached;
    }

    public ShippingLabelMetaDataEntity findCachedByUuid(String uuid) {
        if (uuid == null || uuid.isBlank()) {
            return null;
        }
        return qrShippingLabelCache.findByUuid(uuid);
    }

    public ShippingLabelMetaDataEntity findCachedByTrackingNumber(String trackingNumber) {
        if (trackingNumber == null || trackingNumber.isBlank()) {
            return null;
        }
        String normalized = trackingNumber.trim().toUpperCase(Locale.ROOT);
        return qrShippingLabelCache.findByTrackingNumber(normalized);
    }

    public ShippingLabelMetaDataEntity createShipment(ShippingLabelMetaDataEntity request, DeliveryState initialState) {
        Objects.requireNonNull(request, "request");

        UUID uuid = UUID.randomUUID();
        TrackingNumberMetaDataEntity trackingMeta = trackingService.createTrackingNumber(uuid);
        String trackingNumber = trackingMeta != null ? trackingMeta.getTrackingNumber() : uuid.toString();

        DeliveryState state = initialState != null
                ? initialState
                : (request.getDeliveryState() != null ? request.getDeliveryState() : DeliveryState.LABEL_CREATED);

        ShippingLabelMetaDataEntity entity = new ShippingLabelMetaDataEntity(
                uuid.toString(),
                trackingNumber,
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

        Log.info("Shipment label created: " + trackingNumber);
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
        TrackingNumberMetaDataEntity trackingMeta = trackingService.createTrackingNumber(parsedUuid);
        String trackingNumber = trackingMeta != null ? trackingMeta.getTrackingNumber() : uuid;

        DeliveryState state = initialState != null
                ? initialState
                : (request.getDeliveryState() != null ? request.getDeliveryState() : DeliveryState.LABEL_CREATED);

        ShippingLabelMetaDataEntity entity = new ShippingLabelMetaDataEntity(
                uuid,
                trackingNumber,
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

        Log.info("Shipment label created with UUID: " + uuid);
        persistAsync(entity, PERSIST_DELAY);
        return entity;
    }

    public ShippingLabelMetaDataEntity createShipment(ShippingLabelMetaDataEntity request) {
        return createShipment(request, DeliveryState.LABEL_CREATED);
    }

    public ShippingLabelMetaDataEntity updateDeliveryState(String uuid, DeliveryState targetState) {
        Objects.requireNonNull(uuid, "uuid");
        Objects.requireNonNull(targetState, "targetState");

        ShippingLabelMetaDataEntity entity = qrShippingLabelCache.findByUuid(uuid);
        if (entity == null && !qrShippingLabelCache.isPrimed()) {
            entity = repository.findById(uuid).orElse(null);
        }
        if (entity == null) {
            return null;
        }

        entity.setDeliveryState(targetState);
        Log.info("Shipment delivery state updated: " + uuid + " -> " + targetState);
        persistAsync(entity, Duration.ZERO);
        return entity;
    }

    public boolean deleteShipment(String uuid) {
        if (uuid == null || uuid.isBlank()) {
            return false;
        }
        ShippingLabelMetaDataEntity entity = qrShippingLabelCache.findByUuid(uuid);
        if (entity == null && !qrShippingLabelCache.isPrimed()) {
            entity = repository.findById(uuid).orElse(null);
        }
        if (entity == null) {
            return false;
        }
        String targetUuid = entity.getUuid();
        qrShippingLabelCache.removeShippingLabel(targetUuid);
        deliveryStateCache.removeDeliveryState(targetUuid);
        AsyncTask.runFuture(() -> {
            repository.deleteById(targetUuid);
            Log.info("Shipment label deleted async: " + targetUuid);
            return null;
        });
        return true;
    }

    private void persistAsync(ShippingLabelMetaDataEntity entity, Duration delay) {
        Duration effectiveDelay = delay == null ? Duration.ZERO : delay;
        registerCaches(entity);
        AsyncTask.runFuture(() -> {
            if (!effectiveDelay.isZero()) {
                Thread.sleep(effectiveDelay.toMillis());
            }
            ShippingLabelMetaDataEntity saved = repository.save(entity);
            Log.info("Shipment label persisted async: " + saved.getUuid());
            return saved;
        });
    }

    private void registerCaches(ShippingLabelMetaDataEntity entity) {
        qrShippingLabelCache.registerShippingLabel(entity);
        deliveryStateCache.registerDeliveryState(entity);
    }

    private void registerCaches(Collection<ShippingLabelMetaDataEntity> entities) {
        if (entities == null || entities.isEmpty()) {
            return;
        }
        for (ShippingLabelMetaDataEntity entity : entities) {
            if (entity != null) {
                registerCaches(entity);
            }
        }
    }

    private List<ShippingLabelMetaDataEntity> loadAllFromRepository() {
        try {
            return repository.findAll();
        } catch (Exception ex) {
            Log.warn("Unable to load shipment labels from database: " + ex.getMessage());
            return List.of();
        }
    }

    private void primeCaches(List<ShippingLabelMetaDataEntity> labels) {
        qrShippingLabelCache.primeCache(labels);
        deliveryStateCache.primeCache(labels);
    }

    private void primeCachesFromDatabase() {
        List<ShippingLabelMetaDataEntity> loaded = loadAllFromRepository();
        primeCaches(loaded);
    }

    private void primeCachesAsync() {
        if (!priming.compareAndSet(false, true)) {
            return;
        }
        AsyncTask.runFuture(() -> {
            try {
                primeCachesFromDatabase();
                Log.info("Shipment label cache primed async.");
            } finally {
                priming.set(false);
            }
            return null;
        });
    }

    private ShippingLabelMetaDataEntity syncDeliveryState(ShippingLabelMetaDataEntity label) {
        return label;
    }
}
