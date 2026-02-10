package org.tavall.couriers.api.web.service.camera;

import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.tavall.couriers.api.console.Log;
import org.tavall.couriers.api.delivery.state.DeliveryState;
import org.tavall.couriers.api.qr.scan.LocalQRScanner;
import org.tavall.couriers.api.qr.scan.cache.ScanCacheService;
import org.tavall.couriers.api.qr.scan.cache.ScanErrorCacheService;
import org.tavall.couriers.api.qr.scan.metadata.ScanResponse;
import org.tavall.couriers.api.qr.scan.session.CameraSessionService;
import org.tavall.couriers.api.qr.scan.state.CameraState;
import org.tavall.couriers.api.qr.scan.state.GeminiResponseState;
import org.tavall.couriers.api.web.camera.CameraOptions;
import org.tavall.couriers.api.web.camera.CameraType;
import org.tavall.couriers.api.web.entities.ShippingLabelMetaDataEntity;
import org.tavall.couriers.api.web.service.route.DeliveryRouteService;
import org.tavall.couriers.api.web.service.shipping.ShippingLabelMetaDataService;
import org.tavall.couriers.api.web.user.permission.UserPermissions;
import org.tavall.gemini.clients.response.Gemini3Response;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

@Service
public class CameraPageService {

    private static final String MODE_MERCHANT_INTAKE = "merchant-intake";
    private static final String MODE_DRIVER_STATE = "driver-state";

    private final CameraFrameAnalyzer frameAnalyzer;
    private final ShippingLabelMetaDataService shippingService;
    private final DeliveryRouteService deliveryRouteService;
    private final ScanCacheService scanCache;
    private final ScanErrorCacheService scanErrorCache;
    private final LocalQRScanner localQRScanner;
    private final CameraSessionService cameraSessionService;
    private final AtomicBoolean scanInFlight = new AtomicBoolean(false);
    private final AtomicReference<ScanResponse> scanReady = new AtomicReference<>();
    private final AtomicBoolean intakeInFlight = new AtomicBoolean(false);
    private final AtomicReference<ScanResponse> intakeReady = new AtomicReference<>();
    private final AtomicBoolean routeInFlight = new AtomicBoolean(false);
    private final AtomicReference<ScanResponse> routeReady = new AtomicReference<>();

    public CameraPageService(CameraFrameAnalyzer frameAnalyzer,
                             ShippingLabelMetaDataService shippingService,
                             DeliveryRouteService deliveryRouteService,
                             ScanCacheService scanCache,
                             ScanErrorCacheService scanErrorCache,
                             LocalQRScanner localQRScanner,
                             CameraSessionService cameraSessionService) {
        this.frameAnalyzer = frameAnalyzer;
        this.shippingService = shippingService;
        this.deliveryRouteService = deliveryRouteService;
        this.scanCache = scanCache;
        this.scanErrorCache = scanErrorCache;
        this.localQRScanner = localQRScanner;
        this.cameraSessionService = cameraSessionService;
    }

    public List<ScanResponse> getProcessingResponses() {
        List<ScanResponse> responses = new ArrayList<>();
        if (intakeInFlight.get()) {
            responses.add(pendingResponse("Processing intake scan"));
        }
        if (scanInFlight.get()) {
            responses.add(pendingResponse("Processing scan"));
        }
        return responses;
    }

    public CameraScanResult handleFrame(byte[] snapshot,
                                        CameraOptions cameraOptions,
                                        Authentication authentication,
                                        String routeId,
                                        String scanSessionId) {
        CameraOptions options = cameraOptions != null ? cameraOptions : CameraOptions.defaultOption();
        Log.info("[CameraPageService] Scan received (mode=" + options.mode() + ", type=" + options.cameraType() + ")");
        boolean merchantIntake = options.intakeFlow();
        boolean driverState = options.cameraType() == CameraType.QR_SCAN;
        boolean routeMode = options.cameraType() == CameraType.ROUTE;

        if (merchantIntake && !hasPermission(authentication, UserPermissions.MERCHANT_INTAKE_SCAN)) {
            Log.warn("[CameraPageService] Intake scan blocked: missing permission.");
            return new CameraScanResult(true, errorResponse("Merchant intake scan not permitted"));
        }

        if (driverState) {
            return new CameraScanResult(false, handleDriverStateScan(snapshot, scanSessionId, options.cameraType()));
        }

        if (routeMode) {
            return new CameraScanResult(false, handleRouteScan(snapshot, routeId, scanSessionId, options.cameraType()));
        }

        if (merchantIntake) {
            return new CameraScanResult(false, handleMerchantIntake(snapshot, scanSessionId, options.cameraType()));
        }

        return new CameraScanResult(false, handleStandardScan(snapshot, scanSessionId, options.cameraType()));
    }

    public ScanResponse errorResponse(String message) {
        ScanResponse response = new ScanResponse(
                null,
                CameraState.ERROR,
                GeminiResponseState.ERROR,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                message
        );
        scanErrorCache.registerScanError(response);
        return response;
    }

    public void closeSession(String scanSessionId) {
        cameraSessionService.closeSession(scanSessionId);
    }

    public ScanResponse getIntakeStatus(String scanSessionId) {
        ScanResponse cached = intakeReady.getAndSet(null);
        if (cached != null) {
            Log.info("[CameraPageService] Returning cached intake status response.");
            return applySessionRules(scanSessionId, CameraType.INTAKE, cached);
        }

        if (intakeInFlight.get()) {
            return touchSessionState(scanSessionId, CameraType.INTAKE, processingResponse("Processing intake scan..."));
        }

        return touchSessionState(scanSessionId, CameraType.INTAKE, searchingResponse("Awaiting intake scan..."));
    }

    private ScanResponse handleMerchantIntake(byte[] snapshot, String scanSessionId, CameraType cameraType) {
        ScanResponse cached = intakeReady.getAndSet(null);
        if (cached != null) {
            Log.info("[CameraPageService] Returning cached intake response.");
            return applySessionRules(scanSessionId, cameraType, cached);
        }

        ScanResponse sessionDuplicate = findSessionDuplicateByQr(snapshot, scanSessionId, cameraType);
        if (sessionDuplicate != null) {
            return sessionDuplicate;
        }

        if (!frameAnalyzer.looksLikeDocument(snapshot)) {
            Log.info("[CameraPageService] Intake scan: no document detected.");
            return touchSessionState(scanSessionId, cameraType, searchingResponse("No document in frame"));
        }

        ScanResponse cachedExisting = findCachedExistingByQr(snapshot);
        if (cachedExisting != null) {
            return applySessionRules(scanSessionId, cameraType, cachedExisting);
        }

        if (intakeInFlight.get()) {
            Log.info("[CameraPageService] Intake scan already in flight.");
            ScanResponse local = localProcessingResponse(snapshot, "Processing intake scan...");
            return local != null ? local : touchSessionState(scanSessionId, cameraType, processingResponse("Processing intake scan..."));
        }

        kickOffMerchantIntake(snapshot);
        ScanResponse local = localProcessingResponse(snapshot, "QR detected. Processing intake...");
        return local != null ? local : touchSessionState(scanSessionId, cameraType, foundResponse("Document detected"));
    }

    private ScanResponse handleStandardScan(byte[] snapshot, String scanSessionId, CameraType cameraType) {
        ScanResponse cached = scanReady.getAndSet(null);
        if (cached != null) {
            Log.info("[CameraPageService] Returning cached scan response.");
            return applySessionRules(scanSessionId, cameraType, cached);
        }

        ScanResponse sessionDuplicate = findSessionDuplicateByQr(snapshot, scanSessionId, cameraType);
        if (sessionDuplicate != null) {
            return sessionDuplicate;
        }

        if (!frameAnalyzer.looksLikeDocument(snapshot)) {
            Log.info("[CameraPageService] Scan: no document detected.");
            return touchSessionState(scanSessionId, cameraType, searchingResponse("No document in frame"));
        }

        if (scanInFlight.get()) {
            Log.info("[CameraPageService] Scan already in flight.");
            ScanResponse local = localProcessingResponse(snapshot, "Processing scan...");
            return local != null ? local : touchSessionState(scanSessionId, cameraType, processingResponse("Processing scan..."));
        }

        kickOffStandardScan(snapshot);
        ScanResponse local = localProcessingResponse(snapshot, "QR detected. Processing scan...");
        return local != null ? local : touchSessionState(scanSessionId, cameraType, foundResponse("Document detected"));
    }

    private ScanResponse handleRouteScan(byte[] snapshot, String routeId, String scanSessionId, CameraType cameraType) {
        ScanResponse cached = routeReady.getAndSet(null);
        if (cached != null) {
            return applySessionRules(scanSessionId, cameraType, cached);
        }

        if (routeId == null || routeId.isBlank()) {
            Log.warn("[CameraPageService] Route scan missing routeId.");
            return touchSessionState(scanSessionId, cameraType, errorResponse("Route not specified"));
        }

        ScanResponse sessionDuplicate = findSessionDuplicateByQr(snapshot, scanSessionId, cameraType);
        if (sessionDuplicate != null) {
            return sessionDuplicate;
        }

        if (!frameAnalyzer.looksLikeDocument(snapshot)) {
            Log.info("[CameraPageService] Route scan: no document detected.");
            return touchSessionState(scanSessionId, cameraType, searchingResponse("No document in frame"));
        }

        if (routeInFlight.get()) {
            Log.info("[CameraPageService] Route scan already in flight.");
            ScanResponse local = localProcessingResponse(snapshot, "Processing route scan...");
            return local != null ? local : touchSessionState(scanSessionId, cameraType, processingResponse("Processing route scan..."));
        }

        kickOffRouteScan(snapshot, routeId);
        ScanResponse local = localProcessingResponse(snapshot, "QR detected. Processing route scan...");
        return local != null ? local : touchSessionState(scanSessionId, cameraType, foundResponse("Document detected"));
    }

    private ScanResponse handleDriverStateScan(byte[] snapshot, String scanSessionId, CameraType cameraType) {
        if (snapshot == null || snapshot.length == 0) {
            return touchSessionState(scanSessionId, cameraType, errorResponse("Empty scan frame"));
        }

        Optional<UUID> uuidOpt = localQRScanner.scanForQrCode(snapshot);
        if (uuidOpt.isEmpty()) {
            Log.info("[CameraPageService] Driver scan: no QR code detected.");
            return touchSessionState(scanSessionId, cameraType, searchingResponse("No QR code detected"));
        }

        String uuid = uuidOpt.get().toString();
        if (cameraSessionService.isDuplicate(scanSessionId, cameraType, uuid, null, null)) {
            ShippingLabelMetaDataEntity cached = shippingService.findCachedByUuid(uuid);
            return sessionDuplicateResponse(uuid, cached);
        }
        ShippingLabelMetaDataEntity label = shippingService.findByUuid(uuid);
        if (label == null) {
            Log.warn("[CameraPageService] Driver scan: label not found for UUID " + uuid);
            ScanResponse response = new ScanResponse(
                    uuid,
                    CameraState.ERROR,
                    GeminiResponseState.ERROR,
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    "Label not found"
            );
            scanErrorCache.registerScanError(response);
            return touchSessionState(scanSessionId, cameraType, response);
        }

        Log.info("[CameraPageService] Driver scan matched label " + uuid);
        ScanResponse response = new ScanResponse(
                label.getUuid(),
                CameraState.FOUND,
                GeminiResponseState.COMPLETE,
                label.getTrackingNumber(),
                label.getRecipientName(),
                label.getAddress(),
                label.getCity(),
                label.getState(),
                label.getZipCode(),
                label.getCountry(),
                label.getPhoneNumber(),
                label.getDeliverBy(),
                "Label ready"
        );
        return applySessionRules(scanSessionId, cameraType, response);
    }

    private void kickOffMerchantIntake(byte[] snapshot) {
        if (!intakeInFlight.compareAndSet(false, true)) {
            return;
        }

        Log.info("[CameraPageService] Intake scan started.");
        frameAnalyzer.analyzeFrameAsync(snapshot, false)
                .thenApply(Gemini3Response::getResponse)
                .thenApply(this::buildMerchantIntakeResponse)
                .whenComplete((finalResponse, ex) -> {
                    if (ex != null) {
                        Log.warn("[CameraPageService] Intake scan failed: " + ex.getMessage());
                        intakeReady.set(errorResponse("No intake data"));
                    } else if (finalResponse != null) {
                        Log.info("[CameraPageService] Intake scan complete (" + finalResponse.cameraState() + ").");
                        cacheScanResponse(finalResponse);
                        intakeReady.set(finalResponse);
                    } else {
                        intakeReady.set(errorResponse("No intake data"));
                    }
                    intakeInFlight.set(false);
                });
    }

    private void kickOffStandardScan(byte[] snapshot) {
        if (!scanInFlight.compareAndSet(false, true)) {
            return;
        }

        Log.info("[CameraPageService] Standard scan started.");
        frameAnalyzer.analyzeFrameAsync(snapshot, true)
                .thenApply(Gemini3Response::getResponse)
                .thenApply(this::buildStandardScanResponse)
                .whenComplete((finalResponse, ex) -> {
                    if (ex != null) {
                        Log.warn("[CameraPageService] Standard scan failed: " + ex.getMessage());
                        scanReady.set(errorResponse("No scan response"));
                    } else {
                        if (finalResponse != null) {
                            Log.info("[CameraPageService] Standard scan complete (" + finalResponse.cameraState() + ").");
                            cacheScanResponse(finalResponse);
                        }
                        scanReady.set(finalResponse != null ? finalResponse : errorResponse("No scan response"));
                    }
                    scanInFlight.set(false);
                });
    }

    private void kickOffRouteScan(byte[] snapshot, String routeId) {
        if (!routeInFlight.compareAndSet(false, true)) {
            return;
        }

        Log.info("[CameraPageService] Route scan started for " + routeId);
        frameAnalyzer.analyzeFrameAsync(snapshot, true)
                .thenApply(Gemini3Response::getResponse)
                .thenApply(scanResponse -> buildRouteScanResponse(scanResponse, routeId))
                .whenComplete((finalResponse, ex) -> {
                    if (ex != null) {
                        routeReady.set(errorResponse("Route scan failed"));
                        Log.exception(ex);
                    } else if (finalResponse != null) {
                        Log.info("[CameraPageService] Route scan complete (" + finalResponse.cameraState() + ").");
                        routeReady.set(finalResponse);
                    } else {
                        routeReady.set(errorResponse("Route scan failed"));
                    }
                    routeInFlight.set(false);
                });
    }

    private ScanResponse buildMerchantIntakeResponse(ScanResponse scanResponse) {
        if (scanResponse == null) {
            return errorResponse("No scan data");
        }

        if (scanResponse.cameraState() == CameraState.ERROR) {
            return scanResponse;
        }

        if (scanResponse.cameraState() == CameraState.SEARCHING) {
            return scanResponse;
        }

        ShippingLabelMetaDataEntity existing = findCachedExisting(scanResponse);
        if (existing != null) {
            return new ScanResponse(
                    existing.getUuid(),
                    CameraState.FOUND,
                    GeminiResponseState.COMPLETE,
                    existing.getTrackingNumber(),
                    existing.getRecipientName(),
                    existing.getAddress(),
                    existing.getCity(),
                    existing.getState(),
                    existing.getZipCode(),
                    existing.getCountry(),
                    existing.getPhoneNumber(),
                    existing.getDeliverBy(),
                    "Label already exists",
                    "existing",
                    false,
                    true
            );
        }

        if (!isBlank(scanResponse.uuid())) {
            if (isIntakeIncomplete(scanResponse)) {
                return new ScanResponse(
                        scanResponse.uuid(),
                        CameraState.FOUND,
                        GeminiResponseState.COMPLETE,
                        scanResponse.trackingNumber(),
                        scanResponse.name(),
                        scanResponse.address(),
                        scanResponse.city(),
                        scanResponse.state(),
                        scanResponse.zipCode(),
                        scanResponse.country(),
                        scanResponse.phoneNumber(),
                        scanResponse.deadline(),
                        "Incomplete label data"
                );
            }

            return new ScanResponse(
                    scanResponse.uuid(),
                    CameraState.FOUND,
                    GeminiResponseState.COMPLETE,
                    scanResponse.trackingNumber(),
                    scanResponse.name(),
                    scanResponse.address(),
                    scanResponse.city(),
                    scanResponse.state(),
                    scanResponse.zipCode(),
                    scanResponse.country(),
                    scanResponse.phoneNumber(),
                    scanResponse.deadline(),
                    "Confirm intake to create label",
                    "pending",
                    true,
                    false
            );
        }

        if (isIntakeIncomplete(scanResponse)) {
            return new ScanResponse(
                    null,
                    CameraState.FOUND,
                    GeminiResponseState.COMPLETE,
                    scanResponse.trackingNumber(),
                    scanResponse.name(),
                    scanResponse.address(),
                    scanResponse.city(),
                    scanResponse.state(),
                    scanResponse.zipCode(),
                    scanResponse.country(),
                    scanResponse.phoneNumber(),
                    scanResponse.deadline(),
                    "Incomplete label data"
            );
        }

        return new ScanResponse(
                scanResponse.uuid(),
                CameraState.FOUND,
                GeminiResponseState.COMPLETE,
                scanResponse.trackingNumber(),
                scanResponse.name(),
                scanResponse.address(),
                scanResponse.city(),
                scanResponse.state(),
                scanResponse.zipCode(),
                scanResponse.country(),
                scanResponse.phoneNumber(),
                scanResponse.deadline(),
                "Confirm intake to create label",
                "pending",
                true,
                false
        );
    }

    private ScanResponse buildStandardScanResponse(ScanResponse scanResponse) {
        if (scanResponse == null) {
            return errorResponse("No scan data");
        }

        if (scanResponse.cameraState() == CameraState.ERROR) {
            return scanResponse;
        }

        if (scanResponse.cameraState() == CameraState.SEARCHING) {
            return scanResponse;
        }

        if (isBlank(scanResponse.uuid())) {
            return new ScanResponse(
                    null,
                    CameraState.FOUND,
                    GeminiResponseState.COMPLETE,
                    scanResponse.trackingNumber(),
                    scanResponse.name(),
                    scanResponse.address(),
                    scanResponse.city(),
                    scanResponse.state(),
                    scanResponse.zipCode(),
                    scanResponse.country(),
                    scanResponse.phoneNumber(),
                    scanResponse.deadline(),
                    "Awaiting QR code"
            );
        }

        ShippingLabelMetaDataEntity existing = shippingService.findByUuid(scanResponse.uuid());
        if (existing != null) {
            return new ScanResponse(
                    existing.getUuid(),
                    CameraState.FOUND,
                    GeminiResponseState.COMPLETE,
                    existing.getTrackingNumber(),
                    existing.getRecipientName(),
                    existing.getAddress(),
                    existing.getCity(),
                    existing.getState(),
                    existing.getZipCode(),
                    existing.getCountry(),
                    existing.getPhoneNumber(),
                    existing.getDeliverBy(),
                    "Label already exists",
                    "existing",
                    false,
                    true
            );
        }

        if (isIntakeIncomplete(scanResponse)) {
            return new ScanResponse(
                    scanResponse.uuid(),
                    CameraState.FOUND,
                    GeminiResponseState.COMPLETE,
                    scanResponse.trackingNumber(),
                    scanResponse.name(),
                    scanResponse.address(),
                    scanResponse.city(),
                    scanResponse.state(),
                    scanResponse.zipCode(),
                    scanResponse.country(),
                    scanResponse.phoneNumber(),
                    scanResponse.deadline(),
                    "Incomplete label data"
            );
        }

        ShippingLabelMetaDataEntity request = buildIntakeRequest(scanResponse);
        ShippingLabelMetaDataEntity created = shippingService.createShipmentWithUuid(
                request,
                scanResponse.uuid(),
                DeliveryState.LABEL_CREATED
        );

        return new ScanResponse(
                created.getUuid(),
                CameraState.FOUND,
                GeminiResponseState.COMPLETE,
                created.getTrackingNumber(),
                created.getRecipientName(),
                created.getAddress(),
                created.getCity(),
                created.getState(),
                created.getZipCode(),
                created.getCountry(),
                created.getPhoneNumber(),
                created.getDeliverBy(),
                "Label created from QR intake"
        );
    }

    private ScanResponse buildRouteScanResponse(ScanResponse scanResponse, String routeId) {
        if (scanResponse == null) {
            return errorResponse("No scan data");
        }
        if (scanResponse.cameraState() == CameraState.ERROR) {
            return scanResponse;
        }
        if (scanResponse.cameraState() == CameraState.SEARCHING) {
            return scanResponse;
        }

        if (!isBlank(scanResponse.uuid())) {
            ShippingLabelMetaDataEntity existing = shippingService.findByUuid(scanResponse.uuid());
            if (existing != null) {
                return new ScanResponse(
                        existing.getUuid(),
                        CameraState.FOUND,
                        GeminiResponseState.COMPLETE,
                        existing.getTrackingNumber(),
                        existing.getRecipientName(),
                        existing.getAddress(),
                        existing.getCity(),
                        existing.getState(),
                        existing.getZipCode(),
                        existing.getCountry(),
                        existing.getPhoneNumber(),
                        existing.getDeliverBy(),
                        "Confirm to add to route",
                        "ROUTE_CONFIRM",
                        true,
                        true
                );
            }
        }

        boolean incomplete = isIntakeIncomplete(scanResponse);
        return new ScanResponse(
                scanResponse.uuid(),
                CameraState.FOUND,
                GeminiResponseState.COMPLETE,
                scanResponse.trackingNumber(),
                scanResponse.name(),
                scanResponse.address(),
                scanResponse.city(),
                scanResponse.state(),
                scanResponse.zipCode(),
                scanResponse.country(),
                scanResponse.phoneNumber(),
                scanResponse.deadline(),
                incomplete ? "Waiting for data..." : "Confirm intake to add to route",
                "ROUTE_INTAKE_REQUIRED",
                !incomplete,
                false
        );
    }

    private ShippingLabelMetaDataEntity buildIntakeRequest(ScanResponse scanResponse) {
        ShippingLabelMetaDataEntity request = new ShippingLabelMetaDataEntity();
        if (scanResponse == null) {
            return request;
        }
        request.setRecipientName(scanResponse.name());
        request.setAddress(scanResponse.address());
        request.setCity(scanResponse.city());
        request.setState(scanResponse.state());
        request.setZipCode(scanResponse.zipCode());
        request.setCountry(scanResponse.country());
        request.setPhoneNumber(scanResponse.phoneNumber());
        request.setDeliverBy(scanResponse.deadline());
        request.setDeliveryState(DeliveryState.LABEL_CREATED);
        return request;
    }

    private ScanResponse findCachedExistingByQr(byte[] snapshot) {
        if (snapshot == null || snapshot.length == 0) {
            return null;
        }
        Optional<UUID> uuidOpt = localQRScanner.scanForQrCode(snapshot);
        if (uuidOpt.isEmpty()) {
            return null;
        }
        String uuid = uuidOpt.get().toString();
        ShippingLabelMetaDataEntity existing = shippingService.findCachedByUuid(uuid);
        if (existing == null) {
            return null;
        }
        Log.info("[CameraPageService] Intake scan matched cached label " + uuid);
        return new ScanResponse(
                existing.getUuid(),
                CameraState.SCANNED,
                GeminiResponseState.COMPLETE,
                existing.getTrackingNumber(),
                existing.getRecipientName(),
                existing.getAddress(),
                existing.getCity(),
                existing.getState(),
                existing.getZipCode(),
                existing.getCountry(),
                existing.getPhoneNumber(),
                existing.getDeliverBy(),
                "Label already exists",
                "existing",
                false,
                true
        );
    }

    private ScanResponse findSessionDuplicateByQr(byte[] snapshot, String scanSessionId, CameraType cameraType) {
        if (snapshot == null || snapshot.length == 0 || scanSessionId == null || scanSessionId.isBlank()) {
            return null;
        }
        Optional<UUID> uuidOpt = localQRScanner.scanForQrCode(snapshot);
        if (uuidOpt.isEmpty()) {
            return null;
        }
        String uuid = uuidOpt.get().toString();
        if (!cameraSessionService.isDuplicate(scanSessionId, cameraType, uuid, null, null)) {
            return null;
        }
        ShippingLabelMetaDataEntity cached = shippingService.findCachedByUuid(uuid);
        return sessionDuplicateResponse(uuid, cached);
    }

    private ScanResponse sessionDuplicateResponse(String uuid, ShippingLabelMetaDataEntity cached) {
        String resolvedUuid = uuid;
        if (isBlank(resolvedUuid) && cached != null && !isBlank(cached.getUuid())) {
            resolvedUuid = cached.getUuid();
        }
        return new ScanResponse(
                resolvedUuid,
                CameraState.SCANNED,
                GeminiResponseState.COMPLETE,
                cached != null ? cached.getTrackingNumber() : null,
                cached != null ? cached.getRecipientName() : null,
                cached != null ? cached.getAddress() : null,
                cached != null ? cached.getCity() : null,
                cached != null ? cached.getState() : null,
                cached != null ? cached.getZipCode() : null,
                cached != null ? cached.getCountry() : null,
                cached != null ? cached.getPhoneNumber() : null,
                cached != null ? cached.getDeliverBy() : null,
                "Already scanned in this session",
                "already_scanned",
                false,
                false
        );
    }

    private ShippingLabelMetaDataEntity findCachedExisting(ScanResponse scanResponse) {
        if (scanResponse == null) {
            return null;
        }
        ShippingLabelMetaDataEntity existing = null;
        if (!isBlank(scanResponse.uuid())) {
            existing = shippingService.findCachedByUuid(scanResponse.uuid());
        }
        if (existing == null && !isBlank(scanResponse.trackingNumber())) {
            existing = shippingService.findCachedByTrackingNumber(scanResponse.trackingNumber());
        }
        return existing;
    }

    private String buildAddressKey(ScanResponse response) {
        if (response == null) {
            return null;
        }
        String address = normalizeKey(response.address());
        String city = normalizeKey(response.city());
        String state = normalizeKey(response.state());
        String zip = normalizeKey(response.zipCode());
        String country = normalizeKey(response.country());
        if (address == null || city == null || state == null || zip == null || country == null) {
            return null;
        }
        return String.join("|", address, city, state, zip, country);
    }

    private String normalizeKey(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim().toLowerCase();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private ScanResponse addressDuplicateResponse(ScanResponse response) {
        return new ScanResponse(
                response.uuid(),
                CameraState.SCANNED,
                GeminiResponseState.COMPLETE,
                response.trackingNumber(),
                response.name(),
                response.address(),
                response.city(),
                response.state(),
                response.zipCode(),
                response.country(),
                response.phoneNumber(),
                response.deadline(),
                "Already scanned in this session",
                "already_scanned",
                false,
                false
        );
    }

    private ScanResponse pendingResponse(String message) {
        return new ScanResponse(
                null,
                CameraState.ANALYZING,
                GeminiResponseState.RESPONDING,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                message
        );
    }

    private ScanResponse processingResponse(String message) {
        return new ScanResponse(
                null,
                CameraState.FOUND,
                GeminiResponseState.RESPONDING,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                message,
                "processing",
                true,
                false
        );
    }

    private ScanResponse localProcessingResponse(byte[] snapshot, String message) {
        if (snapshot == null || snapshot.length == 0) {
            return null;
        }
        Optional<UUID> uuidOpt = localQRScanner.scanForQrCode(snapshot);
        if (uuidOpt.isEmpty()) {
            return null;
        }
        String uuid = uuidOpt.get().toString();
        String note = message;
        String shortId = shortUuid(uuid);
        if (shortId != null) {
            note = message + " (" + shortId + ")";
        }
        return new ScanResponse(
                uuid,
                CameraState.FOUND,
                GeminiResponseState.RESPONDING,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                note,
                "processing",
                true,
                false
        );
    }

    private String shortUuid(String uuid) {
        if (uuid == null) {
            return null;
        }
        String trimmed = uuid.replace("-", "");
        if (trimmed.isBlank()) {
            return null;
        }
        if (trimmed.length() <= 6) {
            return trimmed.toUpperCase();
        }
        return trimmed.substring(trimmed.length() - 6).toUpperCase();
    }

    private ScanResponse searchingResponse(String message) {
        return new ScanResponse(
                null,
                CameraState.SEARCHING,
                GeminiResponseState.IDLE,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                message
        );
    }

    private ScanResponse foundResponse(String message) {
        return new ScanResponse(
                null,
                CameraState.FOUND,
                GeminiResponseState.RESPONDING,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                message
        );
    }

    private void cacheScanResponse(ScanResponse response) {
        if (response == null
                || response.cameraState() != CameraState.FOUND
                || isBlank(response.uuid())
                || isIntakeIncomplete(response)) {
            return;
        }
        Log.info("[CameraPageService] Caching scan response for " + response.uuid());
        scanCache.registerScanResponse(response);
    }

    private ScanResponse applySessionRules(String scanSessionId, CameraType cameraType, ScanResponse response) {
        if (response == null) {
            return null;
        }
        cameraSessionService.updateState(scanSessionId, cameraType, response.cameraState());
        if (response.cameraState() != CameraState.FOUND && response.cameraState() != CameraState.SCANNED) {
            return response;
        }

        String addressKey = buildAddressKey(response);
        boolean uuidDuplicate = !isBlank(response.uuid())
                && cameraSessionService.isDuplicate(scanSessionId, cameraType, response.uuid(), null, null);
        boolean trackingDuplicate = !isBlank(response.trackingNumber())
                && cameraSessionService.isDuplicate(scanSessionId, cameraType, null, response.trackingNumber(), null);

        if (uuidDuplicate || trackingDuplicate) {
            return sessionDuplicateResponse(response.uuid(), findCachedExisting(response));
        }

        if (isBlank(response.uuid()) && isBlank(response.trackingNumber())
                && !isBlank(addressKey)
                && cameraSessionService.isDuplicateAddress(scanSessionId, cameraType, addressKey)) {
            return addressDuplicateResponse(response);
        }

        cameraSessionService.registerScan(scanSessionId, cameraType, response, addressKey);
        return response;
    }

    private ScanResponse touchSessionState(String scanSessionId, CameraType cameraType, ScanResponse response) {
        if (response != null) {
            cameraSessionService.updateState(scanSessionId, cameraType, response.cameraState());
        }
        return response;
    }

    private boolean hasPermission(Authentication authentication, UserPermissions permission) {
        if (authentication == null || permission == null) {
            return false;
        }
        String authority = permission.grantedAuthority().getAuthority();
        return authentication.getAuthorities().stream()
                .anyMatch(auth -> auth.getAuthority().equals(authority));
    }

    private boolean isIntakeIncomplete(ScanResponse scanResponse) {
        return isBlank(scanResponse.name())
                || isBlank(scanResponse.address())
                || isBlank(scanResponse.city())
                || isBlank(scanResponse.state())
                || isBlank(scanResponse.zipCode())
                || isBlank(scanResponse.country());
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
