package org.tavall.couriers.api.qr.scan;


import com.google.zxing.BinaryBitmap;
import com.google.zxing.MultiFormatReader;
import com.google.zxing.Result;
import com.google.zxing.client.j2se.BufferedImageLuminanceSource;
import com.google.zxing.common.HybridBinarizer;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.rendering.ImageType;
import org.apache.pdfbox.rendering.PDFRenderer;
import org.springframework.stereotype.Component;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Optional;
import java.util.UUID;

@Component
public class LocalQRScanner {


    private static final float RENDER_DPI = 300;
    public LocalQRScanner(){

    }

    /**
     * Scans a raw byte array (PDF data) for a UUID QR code.
     * This is the method your SmartScanService needs.
     */
    public Optional<UUID> scanPdfForQrCode(byte[] pdfBytes) {
        if (pdfBytes == null || pdfBytes.length == 0) {
            return Optional.empty();
        }

        try (PDDocument document = Loader.loadPDF(pdfBytes)) {
            PDFRenderer pdfRenderer = new PDFRenderer(document);
            BufferedImage image = pdfRenderer.renderImageWithDPI(0, RENDER_DPI, ImageType.BINARY);
            return decodeImage(image);
        } catch (IOException e) {
            return Optional.empty();
        }
    }
    private Optional<UUID> decodeImage(BufferedImage image) {
        if (image == null) return Optional.empty();

        try {
            BufferedImageLuminanceSource source = new BufferedImageLuminanceSource(image);
            BinaryBitmap bitmap = new BinaryBitmap(new HybridBinarizer(source));

            // 1. Decode Raw Text
            Result result = new MultiFormatReader().decode(bitmap);
            String text = result.getText();

            // 2. Validate & Convert (The "Lazy" Way you asked for)
            // If this throws, it's not a UUID, so we catch and return empty.
            return Optional.of(UUID.fromString(text));

        } catch (Exception e) {
            // Catching both ZXing errors (NotFoundException) AND IllegalArgumentException (Bad UUID)
            return Optional.empty();
        }
    }
}