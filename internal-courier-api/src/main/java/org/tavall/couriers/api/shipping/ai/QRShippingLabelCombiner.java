package org.tavall.couriers.api.shipping.ai;


import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.font.PDFont;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject;
import org.tavall.gemini.clients.Gemini3ImageClient;

import java.io.IOException;

public class QRShippingLabelCombiner {



    public QRShippingLabelCombiner() {

    }

    public void createLabel(String qrPath) throws IOException {
        try (PDDocument doc = new PDDocument()) {
            PDPage page = new PDPage(); // Defaults to Letter, you can set to 4x6
            doc.addPage(page);

            try (PDPageContentStream content = new PDPageContentStream(doc, page)) {
                // 1. Draw the QR Code Image (Model tells you where, Code puts it there)
                PDImageXObject pdImage = PDImageXObject.createFromFile(qrPath, doc);
                content.drawImage(pdImage, 50, 600, 150, 150);

                // 2. Draw Text (Data from Gemini, Rendering by Java)
                content.beginText();
                content.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD), 14);                content.newLineAtOffset(50, 550);
                content.showText("Name: Google Gemini");
                content.newLineAtOffset(0, -20);
                content.showText("Deadline: 2/4/26 @ 5PM PST");
                content.endText();
            }
            doc.save("shipping_label.pdf");
        }
    }


}