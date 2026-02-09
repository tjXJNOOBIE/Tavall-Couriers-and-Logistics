package org.tavall.couriers.web.view.controller.shipping.helper;

import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.ui.Model;
import org.tavall.couriers.api.qr.scan.cache.ScanCacheService;
import org.tavall.couriers.api.qr.scan.cache.ScanErrorCacheService;
import org.tavall.couriers.api.qr.scan.metadata.ScanResponse;
import org.tavall.couriers.api.web.entities.ShippingLabelMetaDataEntity;
import org.tavall.couriers.api.web.service.shipping.ShippingLabelDocumentService;
import org.tavall.couriers.api.web.service.shipping.ShippingLabelMetaDataService;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

@Component
public class ShippingLabelPageControllerHelper {

    private final ShippingLabelMetaDataService shippingService;
    private final ShippingLabelDocumentService documentService;
    private final ScanCacheService scanCache;
    private final ScanErrorCacheService scanErrorCache;

    public ShippingLabelPageControllerHelper(ShippingLabelMetaDataService shippingService,
                                             ShippingLabelDocumentService documentService,
                                             ScanCacheService scanCache,
                                             ScanErrorCacheService scanErrorCache) {
        this.shippingService = shippingService;
        this.documentService = documentService;
        this.scanCache = scanCache;
        this.scanErrorCache = scanErrorCache;
    }

    public String render(Model model, String uuid) {
        List<ShippingLabelMetaDataEntity> labels = shippingService.getAllShipmentLabels();

        ShippingLabelMetaDataEntity selected = null;
        if (uuid != null && !uuid.isBlank()) {
            for (ShippingLabelMetaDataEntity l : labels) {
                if (uuid.equals(l.getUuid())) {
                    selected = l;
                    break;
                }
            }
        }

        model.addAttribute("title", "Shipping Labels");
        model.addAttribute("labels", labels);
        model.addAttribute("selected", selected);
        model.addAttribute("selectedUuid", uuid);
        model.addAttribute("deliveryNotes", findDeliveryNotes(selected));

        return "shipping-labels";
    }

    public ResponseEntity<Resource> labelPdf(String uuid) {
        if (uuid == null || uuid.isBlank()) {
            return ResponseEntity.badRequest().build();
        }

        ShippingLabelMetaDataEntity label = shippingService.findByUuid(uuid);
        if (label == null) {
            return ResponseEntity.notFound().build();
        }

        try {
            Path pdfPath = documentService.getOrCreateLabelPdf(label);
            Resource resource = new FileSystemResource(pdfPath);
            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"shipping-label-" + uuid + ".pdf\"")
                    .contentType(MediaType.APPLICATION_PDF)
                    .body(resource);
        } catch (IOException ex) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    private String findDeliveryNotes(ShippingLabelMetaDataEntity selected) {
        if (selected == null) {
            return null;
        }
        List<ScanResponse> responses = new ArrayList<>();
        responses.addAll(scanCache.getRecentResponses(10));
        responses.addAll(scanErrorCache.getRecentErrors(5));
        String uuid = selected.getUuid();
        String trackingNumber = selected.getTrackingNumber();
        for (ScanResponse response : responses) {
            if (response == null) {
                continue;
            }
            if (!matchesScanTarget(response, uuid, trackingNumber)) {
                continue;
            }
            String notes = response.notes();
            if (notes != null && !notes.isBlank()) {
                return notes;
            }
        }
        return null;
    }

    private boolean matchesScanTarget(ScanResponse response, String uuid, String trackingNumber) {
        if (uuid != null && uuid.equals(response.uuid())) {
            return true;
        }
        return trackingNumber != null && trackingNumber.equals(response.trackingNumber());
    }
}
