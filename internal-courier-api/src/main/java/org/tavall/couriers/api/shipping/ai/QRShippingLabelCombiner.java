package org.tavall.couriers.api.shipping.ai;


import com.google.zxing.BinaryBitmap;
import com.google.zxing.MultiFormatReader;
import com.google.zxing.NotFoundException;
import com.google.zxing.Result;
import com.google.zxing.client.j2se.BufferedImageLuminanceSource;
import com.google.zxing.common.HybridBinarizer;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDFont;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject;
import org.apache.pdfbox.pdmodel.graphics.state.PDExtendedGraphicsState;
import org.tavall.couriers.api.console.Log;
import org.tavall.couriers.api.scan.metadata.ScanResponse;
import org.tavall.couriers.api.shipping.ShippingLabelMetaData;
import org.tavall.gemini.clients.Gemini3ImageClient;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Path;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

public class QRShippingLabelCombiner {

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("MM/dd/yyyy hh:mm a")
            .withZone(ZoneId.of("America/Los_Angeles"));
    private final PDType1Font FONT_BOLD = new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD);
    private final PDType1Font FONT_REG = new PDType1Font(Standard14Fonts.FontName.HELVETICA);
    private final String LOGO_FILENAME = "tavall_couriers_logo.png";
    public QRShippingLabelCombiner() {

    }


    public void createLabel(String qrPath, ShippingLabelMetaData data, Path outputPath) throws IOException {

        // Scan logic
        String scannedUuid = "UNKNOWN";
        try {
            scannedUuid = extractDataFromQR(qrPath);
            if (scannedUuid.length() > 50) scannedUuid = scannedUuid.substring(0, 47) + "...";
        } catch (Exception e) {
            // System.err.println("QR Decode Warning: " + e.getMessage()); // Keep logs clean for now
            scannedUuid = "SCAN_ERROR";
        }

        try (PDDocument doc = new PDDocument()) {
            PDPage page = new PDPage(PDRectangle.LETTER);
            doc.addPage(page);

            try (PDPageContentStream content = new PDPageContentStream(doc, page)) {

                // =========================================
                // 0. BACKGROUND WATERMARK (MUST BE DRAWN FIRST)
                // =========================================
                if (new File(LOGO_FILENAME).exists()) {
                    try {
                        PDImageXObject logoImg = PDImageXObject.createFromFile(LOGO_FILENAME, doc);

                        // Set Opacity to 20% (0.2f)
                        PDExtendedGraphicsState graphicsState = new PDExtendedGraphicsState();
//                        graphicsState.setNonStrokingAlphaConstant(0.8f);
                        content.setGraphicsStateParameters(graphicsState);

                        // Calculate Centering & Scaling
                        float pageWidth = page.getMediaBox().getWidth();
                        float pageHeight = page.getMediaBox().getHeight();

                        // Define target width as 70% of page width for a "huge" look
                        float targetWidth = pageWidth * 0.7f;
                        // Calculate height based on image aspect ratio to prevent distortion
                        float scaleFactor = targetWidth / logoImg.getWidth();
                        float targetHeight = logoImg.getHeight() * scaleFactor;

                        float x = (pageWidth - targetWidth) / 2;
                        float y = (pageHeight - targetHeight) / 2;

                        // Draw the faded image center page
                        content.drawImage(logoImg, x, y, targetWidth, targetHeight);

                        // IMPORTANT: Reset Opacity back to 100% (1.0f) for the rest of the document
                        graphicsState.setNonStrokingAlphaConstant(1.0f);
                        content.setGraphicsStateParameters(graphicsState);

                    } catch (IOException e) {
                        System.out.println("Watermark image found but failed to load. Skipping.");
                    }
                }
                // =========================================
                // END WATERMARK
                // =========================================


                float margin = 50;
                float width = PDRectangle.LETTER.getWidth() - (2 * margin);
                float topY = 700;

                // --- DRAW BORDERS ---
                content.setStrokingColor(Color.BLACK);
                content.setLineWidth(1.5f);
                content.addRect(margin, topY - 400, width, 400);
                content.stroke();

                drawLine(content, margin, topY - 50, width);
                drawLine(content, margin, topY - 250, width);
                drawLine(content, margin, topY - 330, width);

                content.moveTo(margin + 300, topY);
                content.lineTo(margin + 300, topY - 50);
                content.stroke();

                // --- 1. HEADER (Standard Text Now) ---
                drawText(content, FONT_BOLD, 8, margin + 5, topY - 12, "FROM:");
                drawText(content, FONT_BOLD, 12, margin + 5, topY - 28, "TAVALL COURIERS HQ");
                drawText(content, FONT_REG, 8, margin + 5, topY - 42, "123 Java Stream Blvd, Colton, CA 92324");

                // Top Right: Date & Weight
                drawText(content, FONT_BOLD, 14, margin + 310, topY - 20, "1 LBS");
                drawText(content, FONT_REG, 10, margin + 310, topY - 35, DATE_FMT.format(data.getDeliverBy()));

                // --- 2. SHIP TO ---
                float shipToY = topY - 80;
                drawText(content, FONT_BOLD, 10, margin + 20, shipToY, "SHIP TO:");

                drawText(content, FONT_REG, 14, margin + 40, shipToY - 30, data.getRecipientName());
                drawText(content, FONT_REG, 14, margin + 40, shipToY - 50, data.getAddress());
                drawText(content, FONT_REG, 14, margin + 40, shipToY - 70,
                        String.format("%s, %s %s", data.getCity(), data.getState(), data.getZipCode()));
                drawText(content, FONT_REG, 14, margin + 40, shipToY - 90, data.getCountry());

                // --- 3. QR IMAGE (Embedded in label) ---
                try {
                    PDImageXObject qrImg = PDImageXObject.createFromFile(qrPath, doc);
                    content.drawImage(qrImg, margin + 350, topY - 240, 100, 100);
                } catch (IOException e) {
                    drawText(content, FONT_BOLD, 12, margin + 350, topY - 150, "[QR MISSING]");
                }

                // --- 4. TRACKING & PRIORITY ---
                float trackY = topY - 280;
                if (data.isPriority()) {
                    content.setNonStrokingColor(Color.BLACK);
                    content.addRect(margin + 20, trackY - 30, 100, 20);
                    content.fill();
                    content.setNonStrokingColor(Color.WHITE);
                    drawText(content, FONT_BOLD, 12, margin + 25, trackY - 25, "PRIORITY");
                    content.setNonStrokingColor(Color.BLACK);
                } else {
                    drawText(content, FONT_BOLD, 16, margin + 20, trackY - 25, "G");
                }

                drawText(content, FONT_REG, 10, margin + 150, trackY - 10, "TRACKING #:");
                drawText(content, FONT_BOLD, 18, margin + 150, trackY - 30, data.getTrackingNumber());

                // --- 5. FOOTER (Scanned Data) ---
                drawText(content, FONT_REG, 8, margin + 10, topY - 390, "REF: " + scannedUuid);
                drawText(content, FONT_REG, 8, margin + 250, topY - 390, "Generated by Tavall AI Engine");
            }
            doc.save(outputPath.toFile());
        }
    }
    private String extractDataFromQR(String filePath) throws IOException, NotFoundException {
        File file = new File(filePath);
        if (!file.exists()) {
            throw new IOException("QR File not found: " + filePath);
        }

        BufferedImage bufferedImage = ImageIO.read(new FileInputStream(file));
        if (bufferedImage == null) {
            throw new IOException("Could not read image: " + filePath);
        }

        // Convert to ZXing's BinaryBitmap
        BinaryBitmap bitmap = new BinaryBitmap(
                new HybridBinarizer(new BufferedImageLuminanceSource(bufferedImage)));

        // Decode
        Result result = new MultiFormatReader().decode(bitmap);
        return result.getText();
    }
    // Helper to draw horizontal lines easily
    private void drawLine(PDPageContentStream content, float x, float y, float width) throws IOException {
        content.moveTo(x, y);
        content.lineTo(x + width, y);
        content.stroke();
    }

    // Helper to draw text easily without setting up beginText/endText every time
    private void drawText(PDPageContentStream content, PDType1Font font, int size, float x, float y, String text) throws IOException {
        content.beginText();
        content.setFont(font, size);
        content.newLineAtOffset(x, y);
        content.showText(text != null ? text : "");
        content.endText();
    }

}