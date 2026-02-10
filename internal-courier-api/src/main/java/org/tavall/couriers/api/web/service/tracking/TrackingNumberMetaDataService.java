package org.tavall.couriers.api.web.service.tracking;

import jakarta.annotation.PostConstruct;
import org.springframework.stereotype.Service;
import org.tavall.couriers.api.concurrent.AsyncTask;
import org.tavall.couriers.api.console.Log;
import org.tavall.couriers.api.tracking.TrackingNumberManager;
import org.tavall.couriers.api.tracking.cache.TrackingNumberCache;
import org.tavall.couriers.api.tracking.metadata.TrackingNumberMetaData;
import org.tavall.couriers.api.web.entities.tracking.TrackingNumberMetaDataEntity;
import org.tavall.couriers.api.web.repositories.TrackingNumberMetaDataRepository;

import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;

@Service
public class TrackingNumberMetaDataService {

    private final TrackingNumberMetaDataRepository repository;
    private final TrackingNumberCache trackingNumberCache;
    private final AtomicBoolean priming = new AtomicBoolean(false);

    public TrackingNumberMetaDataService(TrackingNumberMetaDataRepository repository,
                                         TrackingNumberCache trackingNumberCache) {
        this.repository = repository;
        this.trackingNumberCache = trackingNumberCache;
    }

    @PostConstruct
    public void warmCaches() {
        primeCachesFromDatabase();
    }

    public List<TrackingNumberMetaDataEntity> getAllTrackingNumbers() {
        List<TrackingNumberMetaDataEntity> cached = trackingNumberCache.getAllTrackingNumbers();
        if (!cached.isEmpty() || trackingNumberCache.isPrimed()) {
            return cached;
        }
        primeCachesAsync();
        return cached;
    }

    public TrackingNumberMetaDataEntity findByTrackingNumber(String trackingNumber) {
        if (trackingNumber == null || trackingNumber.isBlank()) {
            return null;
        }
        String normalized = normalizeTrackingNumber(trackingNumber);
        TrackingNumberMetaDataEntity cached = trackingNumberCache.findByTrackingNumber(normalized);
        if (cached != null || trackingNumberCache.isPrimed()) {
            return cached;
        }
        primeCachesAsync();
        return cached;
    }

    public List<TrackingNumberMetaDataEntity> findByTrackingNumbers(Collection<String> trackingNumbers) {
        if (trackingNumbers == null || trackingNumbers.isEmpty()) {
            return List.of();
        }
        List<String> normalized = trackingNumbers.stream()
                .map(this::normalizeTrackingNumber)
                .filter(val -> val != null && !val.isBlank())
                .distinct()
                .toList();
        if (normalized.isEmpty()) {
            return List.of();
        }
        List<TrackingNumberMetaDataEntity> cached = trackingNumberCache.findByTrackingNumbers(normalized);
        if (!cached.isEmpty() || trackingNumberCache.isPrimed()) {
            return cached;
        }
        primeCachesAsync();
        return cached;
    }

    public TrackingNumberMetaDataEntity findByQrUuid(UUID qrUuid) {
        if (qrUuid == null) {
            return null;
        }
        TrackingNumberMetaDataEntity cached = trackingNumberCache.findByQrUuid(qrUuid);
        if (cached != null || trackingNumberCache.isPrimed()) {
            return cached;
        }
        primeCachesAsync();
        return cached;
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

        Log.info("Tracking number created: " + trackingMeta.trackingNumber());
        persistAsync(entity);
        return entity;
    }

    public TrackingNumberMetaDataEntity updateQrUuid(String trackingNumber, UUID targetQrUuid) {
        Objects.requireNonNull(trackingNumber, "trackingNumber");
        Objects.requireNonNull(targetQrUuid, "targetQrUuid");

        String normalized = normalizeTrackingNumber(trackingNumber);
        if (normalized == null) {
            return null;
        }
        TrackingNumberMetaDataEntity entity = trackingNumberCache.findByTrackingNumber(normalized);
        if (entity == null && !trackingNumberCache.isPrimed()) {
            entity = repository.findByTrackingNumber(normalized);
        }
        if (entity == null) {
            return null;
        }

        entity.setQrUuid(targetQrUuid);
        Log.info("Tracking number QR updated: " + normalized);
        persistAsync(entity);
        return entity;
    }

    private void persistAsync(TrackingNumberMetaDataEntity entity) {
        AsyncTask.runFuture(() -> {
            registerCaches(entity);
            TrackingNumberMetaDataEntity saved = repository.save(entity);
            Log.info("Tracking number persisted async: " + saved.getTrackingNumber());
            return saved;
        });
    }

    private void registerCaches(TrackingNumberMetaDataEntity entity) {
        trackingNumberCache.registerTrackingNumber(entity);
    }

    private void registerCaches(Collection<TrackingNumberMetaDataEntity> entities) {
        if (entities == null || entities.isEmpty()) {
            return;
        }
        for (TrackingNumberMetaDataEntity entity : entities) {
            if (entity != null) {
                registerCaches(entity);
            }
        }
    }

    private List<TrackingNumberMetaDataEntity> loadAllFromRepository() {
        try {
            return repository.findAll();
        } catch (Exception ex) {
            Log.warn("Unable to load tracking numbers from database: " + ex.getMessage());
            return List.of();
        }
    }

    private void primeCachesFromDatabase() {
        List<TrackingNumberMetaDataEntity> loaded = loadAllFromRepository();
        primeCaches(loaded);
    }

    private void primeCachesAsync() {
        if (!priming.compareAndSet(false, true)) {
            return;
        }
        AsyncTask.runFuture(() -> {
            try {
                primeCachesFromDatabase();
                Log.info("Tracking number cache primed async.");
            } finally {
                priming.set(false);
            }
            return null;
        });
    }

    private void primeCaches(Collection<TrackingNumberMetaDataEntity> entities) {
        trackingNumberCache.primeCache(entities);
    }

    private String normalizeTrackingNumber(String trackingNumber) {
        if (trackingNumber == null || trackingNumber.isBlank()) {
            return null;
        }
        return trackingNumber.trim().toUpperCase(Locale.ROOT);
    }
}
