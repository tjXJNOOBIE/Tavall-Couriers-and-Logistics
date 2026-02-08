package org.tavall.couriers.api.web.service.tracking;

import org.springframework.stereotype.Service;
import org.tavall.couriers.api.concurrent.AsyncTask;
import org.tavall.couriers.api.tracking.TrackingNumberManager;
import org.tavall.couriers.api.tracking.cache.TrackingNumberCache;
import org.tavall.couriers.api.tracking.metadata.TrackingNumberMetaData;
import org.tavall.couriers.api.web.entities.tracking.TrackingNumberMetaDataEntity;
import org.tavall.couriers.api.web.repositories.TrackingNumberMetaDataRepository;

import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

@Service
public class TrackingNumberMetaDataService {

    private final TrackingNumberMetaDataRepository repository;
    private final TrackingNumberCache trackingNumberCache;

    public TrackingNumberMetaDataService(TrackingNumberMetaDataRepository repository,
                                         TrackingNumberCache trackingNumberCache) {
        this.repository = repository;
        this.trackingNumberCache = trackingNumberCache;
    }

    public List<TrackingNumberMetaDataEntity> getAllTrackingNumbers() {
        return repository.findAll();
    }

    public TrackingNumberMetaDataEntity findByTrackingNumber(String trackingNumber) {
        if (trackingNumber == null || trackingNumber.isBlank()) {
            return null;
        }
        return repository.findByTrackingNumber(trackingNumber);
    }

    public List<TrackingNumberMetaDataEntity> findByTrackingNumbers(Collection<String> trackingNumbers) {
        if (trackingNumbers == null || trackingNumbers.isEmpty()) {
            return List.of();
        }
        return repository.findByTrackingNumberIn(trackingNumbers);
    }

    public TrackingNumberMetaDataEntity findByQrUuid(UUID qrUuid) {
        if (qrUuid == null) {
            return null;
        }
        return repository.findByQrUuid(qrUuid);
    }

    public TrackingNumberMetaDataEntity createTrackingNumber(TrackingNumberMetaDataEntity request) {
        Objects.requireNonNull(request, "request");
        return createTrackingNumber(request.getQrUuid());
    }

    public TrackingNumberMetaDataEntity createTrackingNumber(UUID qrUuid) {
        Objects.requireNonNull(qrUuid, "qrUuid");

        TrackingNumberManager trackingManager = new TrackingNumberManager();
        TrackingNumberMetaData trackingMeta = trackingManager.createTrackingNumber(qrUuid);

        TrackingNumberMetaDataEntity entity = new TrackingNumberMetaDataEntity(
                trackingMeta.trackingNumber(),
                trackingMeta.qrUuid()
        );

        persistAsync(entity);
        return entity;
    }

    public TrackingNumberMetaDataEntity updateQrUuid(String trackingNumber, UUID targetQrUuid) {
        Objects.requireNonNull(trackingNumber, "trackingNumber");
        Objects.requireNonNull(targetQrUuid, "targetQrUuid");

        TrackingNumberMetaDataEntity entity = repository.findByTrackingNumber(trackingNumber);
        if (entity == null) {
            return null;
        }

        entity.setQrUuid(targetQrUuid);
        persistAsync(entity);
        return entity;
    }

    private void persistAsync(TrackingNumberMetaDataEntity entity) {
        AsyncTask.runFuture(() -> {
            registerCaches(entity);
            return repository.save(entity);
        });
    }

    private void registerCaches(TrackingNumberMetaDataEntity entity) {
        trackingNumberCache.registerTrackingNumber(entity);
    }
}
