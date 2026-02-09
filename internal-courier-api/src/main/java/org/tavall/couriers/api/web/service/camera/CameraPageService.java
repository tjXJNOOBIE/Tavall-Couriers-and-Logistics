package org.tavall.couriers.api.web.service.camera;

import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.tavall.couriers.api.console.Log;
import org.tavall.couriers.api.delivery.state.DeliveryState;
import org.tavall.couriers.api.qr.scan.LocalQRScanner;
import org.tavall.couriers.api.qr.scan.cache.ScanCacheService;
import org.tavall.couriers.api.qr.scan.cache.ScanErrorCacheService;
import org.tavall.couriers.api.qr.scan.metadata.ScanResponse;
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
                             LocalQRScanner localQRScanner) {
        this.frameAnalyzer = frameAnalyzer;
        this.shippingService = shippingService;
        this.deliveryRouteService = deliveryRouteService;
        this.scanCache = scanCache;
        this.scanErrorCache = scanErrorCache;
        this.localQRScanner = localQRScanner;
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

    public CameraScanResult handleFrame(byte[] snapshot, CameraOptions cameraOptions, Authentication authentication, String routeId) {
        CameraOptions options = cameraOptions != null ? cameraOptions : CameraOptions.defaultOption();
        Log.info("[CameraPageService] Scan received (mode=" + options.mode() + ", type=" + options.cameraType() + ")");
        boolean merchantIntake = options.intakeFlow();
        boolean driverState = options.cameraType() == CameraType.QR_SCAN;
        boolean routeMode = options.cameraType() == CameraType.ROUTE;

        if (merchantIntake && !hasPermission(authentication, UserPermissions.MERCHANT_INTAKE_SCAN)) {
            return new CameraScanResult(true, errorResponse("Merchant intake scan not permitted"));
        }

        if (driverState) {
            return new CameraScanResult(false, handleDriverStateScan(snapshot));
        }

        if (routeMode) {
            return new CameraScanResult(false, handleRouteScan(snapshot, routeId));
        }

        if (merchantIntake) {
            return new CameraScanResult(false, handleMerchantIntake(snapshot));
        }

        return new CameraScanResult(false, handleStandardScan(snapshot));
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

    private ScanResponse handleMerchantIntake(byte[] snapshot) {
        ScanResponse cached = intakeReady.getAndSet(null);
        if (cached != null) {
            return cached;
        }

        if (intakeInFlight.get()) {
            return pendingResponse("Processing intake scan");
        }

        if (!frameAnalyzer.looksLikeDocument(snapshot)) {
            return searchingResponse("No document in frame");
        }

        kickOffMerchantIntake(snapshot);
        return foundResponse("Document detected");
    }

    private ScanResponse handleStandardScan(byte[] snapshot) {
        ScanResponse cached = scanReady.getAndSet(null);
        if (cached != null) {
            return cached;
        }

        if (scanInFlight.get()) {
            return pendingResponse("Processing scan");
        }

        if (!frameAnalyzer.looksLikeDocument(snapshot)) {
            return searchingResponse("No document in frame");
        }

        kickOffStandardScan(snapshot);
        return foundResponse("Document detected");
    }

    private ScanResponse handleRouteScan(byte[] snapshot, String routeId) {
        ScanResponse cached = routeReady.getAndSet(null);
        if (cached != null) {
            return cached;
        }

        if (routeInFlight.get()) {
            return pendingResponse("Processing route scan");
        }

        if (routeId == null || routeId.isBlank()) {
            return errorResponse("Route not specified");
        }

        if (!frameAnalyzer.looksLikeDocument(snapshot)) {
            return searchingResponse("No document in frame");
        }

        kickOffRouteScan(snapshot, routeId);
        return foundResponse("Document detected");
    }

    private ScanResponse handleDriverStateScan(byte[] snapshot) {
        if (snapshot == null || snapshot.length == 0) {
            return errorResponse("Empty scan frame");
        }

        Optional<UUID> uuidOpt = localQRScanner.scanForQrCode(snapshot);
        if (uuidOpt.isEmpty()) {
            return searchingResponse("No QR code detected");
        }

        String uuid = uuidOpt.get().toString();
        ShippingLabelMetaDataEntity label = shippingService.findByUuid(uuid);
        if (label == null) {
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
            return response;
        }

        return new ScanResponse(
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
    }

    private void kickOffMerchantIntake(byte[] snapshot) {
        if (!intakeInFlight.compareAndSet(false, true)) {
            return;
        }

        frameAnalyzer.analyzeFrameAsync(snapshot, false)
                .thenApply(Gemini3Response::getResponse)
                .thenApply(this::buildMerchantIntakeResponse)
                .whenComplete((finalResponse, ex) -> {
                    if (ex != null) {
                        intakeReady.set(errorResponse("No intake data"));
                    } else if (finalResponse != null) {
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

        frameAnalyzer.analyzeFrameAsync(snapshot, true)
                .thenApply(Gemini3Response::getResponse)
                .thenApply(this::buildStandardScanResponse)
                .whenComplete((finalResponse, ex) -> {
                    if (ex != null) {
                        scanReady.set(errorResponse("No scan response"));
                    } else {
                        if (finalResponse != null) {
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

        frameAnalyzer.analyzeFrameAsync(snapshot, true)
                .thenApply(Gemini3Response::getResponse)
                .thenApply(scanResponse -> buildRouteScanResponse(scanResponse, routeId))
                .whenComplete((finalResponse, ex) -> {
                    if (ex != null) {
                        routeReady.set(errorResponse("Route scan failed"));
                        Log.exception(ex);
                    } else if (finalResponse != null) {
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
                        "Label found"
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
                    "Intake label created"
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

        ShippingLabelMetaDataEntity request = buildIntakeRequest(scanResponse);
        ShippingLabelMetaDataEntity created = shippingService.createShipment(request, DeliveryState.LABEL_CREATED);

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
                "Intake label created"
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
                    "Label found"
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
                deliveryRouteService.addStopAsync(routeId, existing.getUuid());
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
                        "Label added to route",
                        "ROUTE_ADD",
                        false,
                        true
                );
            }
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
                isIntakeIncomplete(scanResponse) ? "Waiting for data…" : "Confirm intake to add to route",
                "ROUTE_INTAKE_REQUIRED",
                true,
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
        scanCache.registerScanResponse(response);
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