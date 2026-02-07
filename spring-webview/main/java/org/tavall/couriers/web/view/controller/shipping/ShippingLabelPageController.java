package org.tavall.couriers.web.view.controller.shipping;

import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.tavall.couriers.api.web.entities.ShippingLabelMetaDataEntity;
import org.tavall.couriers.api.web.endpoints.Routes;
import org.tavall.couriers.api.web.service.shipping.ShippingLabelDocumentService;
import org.tavall.couriers.api.web.service.shipping.ShippingLabelMetaDataService;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

@Controller
public class ShippingLabelPageController {

    private final ShippingLabelMetaDataService shippingService;
    private final ShippingLabelDocumentService documentService;

    public ShippingLabelPageController(ShippingLabelMetaDataService shippingService,
                                       ShippingLabelDocumentService documentService) {
        this.shippingService = shippingService;
        this.documentService = documentService;
    }

    @GetMapping(Routes.SHIPPING_LABELS)
    public String list(Model model) {
        return render(model, null);
    }

    @GetMapping(Routes.SHIPPING_LABEL_DETAIL_TEMPLATE)
    public String detail(@PathVariable("uuid") String uuid, Model model) {
        return render(model, uuid);
    }

    @GetMapping(value = Routes.SHIPPING_LABEL_PDF_TEMPLATE, produces = MediaType.APPLICATION_PDF_VALUE)
    public ResponseEntity<Resource> labelPdf(@PathVariable("uuid") String uuid) {
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

    private String render(Model model, String uuid) {
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

        return "shipping-labels";
    }
}
