package org.tavall.couriers.api.web.service.shipping;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.MultiFormatWriter;
import com.google.zxing.WriterException;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import org.springframework.stereotype.Service;
import org.tavall.couriers.api.delivery.state.DeliveryState;
import org.tavall.couriers.api.shipping.helpers.QRShippingLabelCombiner;
import org.tavall.couriers.api.shipping.metadata.ShippingLabelMetaData;
import org.tavall.couriers.api.web.entities.ShippingLabelMetaDataEntity;
import org.tavall.couriers.api.web.service.hq.HqLocationService;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.Objects;

@Service
public class ShippingLabelDocumentService {

    private static final String LABEL_DIR_NAME = "labels";
    private static final String QR_DIR_NAME = "qr";
    private static final String LABEL_PREFIX = "shipping-label-";
    private static final String QR_PREFIX = "qr-";
    private static final String LABEL_EXTENSION = ".pdf";
    private static final String QR_EXTENSION = ".png";
    private static final int QR_SIZE = 300;

    private final HqLocationService hqLocationService;
    private final QRShippingLabelCombiner combiner = new QRShippingLabelCombiner();
    private final Path labelDir;
    private final Path qrDir;

    public ShippingLabelDocumentService(HqLocationService hqLocationService) {
        this.hqLocationService = hqLocationService;
        Path baseDir = Path.of(System.getProperty("java.io.tmpdir"), "tavall-couriers");
        this.labelDir = baseDir.resolve(LABEL_DIR_NAME);
        this.qrDir = baseDir.resolve(QR_DIR_NAME);
    }

    public Path getOrCreateLabelPdf(ShippingLabelMetaDataEntity entity) throws IOException {
        Objects.requireNonNull(entity, "entity");

        String uuid = entity.getUuid();
        if (uuid == null || uuid.isBlank()) {
            throw new IllegalArgumentException("Label UUID is required to generate PDF.");
        }

        Path pdfPath = labelDir.resolve(LABEL_PREFIX + uuid + LABEL_EXTENSION);
        if (Files.exists(pdfPath)) {
            return pdfPath;
        }

        Files.createDirectories(labelDir);
        Path qrPath = ensureQrCode(uuid);
        ShippingLabelMetaData metaData = toMetaData(entity);

        String fromName = hqLocationService != null ? hqLocationService.resolveFromName() : null;
        String fromAddress = hqLocationService != null ? hqLocationService.resolveFromAddressLine() : null;
        combiner.createLabel(qrPath.toString(), metaData, pdfPath, fromName, fromAddress);
        return pdfPath;
    }

    private Path ensureQrCode(String uuid) throws IOException {
        Path qrPath = qrDir.resolve(QR_PREFIX + uuid + QR_EXTENSION);
        if (Files.exists(qrPath)) {
            return qrPath;
        }

        Files.createDirectories(qrDir);
        try {
            BitMatrix matrix = new MultiFormatWriter().encode(uuid, BarcodeFormat.QR_CODE, QR_SIZE, QR_SIZE);
            MatrixToImageWriter.writeToPath(matrix, "png", qrPath);
        } catch (WriterException e) {
            throw new IOException("Failed to generate QR code for label: " + uuid, e);
        }

        return qrPath;
    }

    private ShippingLabelMetaData toMetaData(ShippingLabelMetaDataEntity entity) {
        Instant deliverBy = entity.getDeliverBy() != null ? entity.getDeliverBy() : Instant.now();
        DeliveryState deliveryState = entity.getDeliveryState() != null
                ? entity.getDeliveryState()
                : DeliveryState.LABEL_CREATED;

        return new ShippingLabelMetaData(
                entity.getUuid(),
                entity.getTrackingNumber(),
                entity.getRecipientName(),
                entity.getPhoneNumber(),
                entity.getAddress(),
                entity.getCity(),
                entity.getState(),
                entity.getZipCode(),
                entity.getCountry(),
                entity.isPriority(),
                deliverBy,
                deliveryState
        );
    }
}
