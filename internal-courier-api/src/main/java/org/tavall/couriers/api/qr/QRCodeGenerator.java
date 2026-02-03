package org.tavall.couriers.api.qr;


import com.google.zxing.BarcodeFormat;
import com.google.zxing.MultiFormatWriter;
import com.google.zxing.WriterException;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import org.tavall.couriers.api.qr.enums.QRState;
import org.tavall.couriers.api.qr.enums.QRType;
import org.tavall.couriers.api.utils.uuid.GenerateUUID;

import java.io.IOException;
import java.nio.file.Path;
import java.time.Instant;

public class QRCodeGenerator {

    String GENERATION_FILE_PATH = System.getProperty("user.dir");
    Path outDir = Path.of(GENERATION_FILE_PATH);
    String IMAGE_FORMAT = "png";
    BarcodeFormat QR_CODE_FORMAT = BarcodeFormat.QR_CODE;
    QRType QR_TYPE  = QRType.UUID;
    int QR_WIDTH = 300;
    int QR_HEIGHT = 300;


    //For java doc, method can be created with a UUID, or data. UUID or data can be null
    // but not both.
    public QRMetaData createQRCodeWithUUID(GenerateUUID newGeneratedUUID, String imageFormat, BarcodeFormat barcodeFormat, int qrWidth, int qrHeight) throws IOException, WriterException {
        QR_TYPE = QRType.UUID;
        if (newGeneratedUUID == null) {
            newGeneratedUUID = new GenerateUUID();
            newGeneratedUUID.generateUUID();

        }
        String qrUUID = newGeneratedUUID.getGeneratedUUID().toString();
        BitMatrix matrix = new MultiFormatWriter().encode(qrUUID, barcodeFormat, qrWidth, qrHeight);
        Path outputFile = outDir.resolve("qr-" + qrUUID + ".png");

        MatrixToImageWriter.writeToPath(matrix, imageFormat, outputFile);
        return new QRMetaData(QR_TYPE, newGeneratedUUID, null, Instant.now(), QRState.ACTIVE);
    }


    public QRMetaData createQRCodeWithData(String data, String imageFormat, BarcodeFormat barcodeFormat, int qrWidth, int qrHeight) throws IOException, WriterException {
        QR_TYPE = QRType.CUSTOM;

        //TODO: Add logging to tell user output path
        BitMatrix matrix = new MultiFormatWriter().encode(data, barcodeFormat, qrWidth, qrHeight);
        Path outputFile = outDir.resolve("qr-" + data + ".png");

        MatrixToImageWriter.writeToPath(matrix, imageFormat, outputFile);
        return new QRMetaData(QR_TYPE, null, data, Instant.now(), QRState.ACTIVE);
    }

    //Default implementation of createQRCodeWithUUID method, class fields are set to default values
    public QRMetaData createQRCodeWithUUID() throws IOException, WriterException {

        return createQRCodeWithUUID(new GenerateUUID(), IMAGE_FORMAT, QR_CODE_FORMAT, QR_WIDTH, QR_HEIGHT);
    }
    // custom path only
    public QRMetaData createQRCodeWithUUID(String path) throws IOException, WriterException {

        return createQRCodeWithUUID(new GenerateUUID(), IMAGE_FORMAT, QR_CODE_FORMAT, QR_WIDTH, QR_HEIGHT);
    }

    //custom height and width
    public QRMetaData createQRCodeWithUUID(int width, int height) throws IOException, WriterException {

        return createQRCodeWithUUID(new GenerateUUID(), IMAGE_FORMAT, QR_CODE_FORMAT, width, height);
    }
    //qr with custom data, default data from class fields
    public QRMetaData createQRCodeWithData(String data) throws IOException, WriterException {

        return createQRCodeWithData(data, IMAGE_FORMAT, QR_CODE_FORMAT, QR_WIDTH, QR_HEIGHT);
    }

    public QRMetaData createQRCodeWithData(String data, int width, int height) throws IOException, WriterException {

        return createQRCodeWithData(data, IMAGE_FORMAT, QR_CODE_FORMAT, width, height);
    }




}