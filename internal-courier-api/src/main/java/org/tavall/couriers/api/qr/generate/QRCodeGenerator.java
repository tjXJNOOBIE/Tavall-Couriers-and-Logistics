package org.tavall.couriers.api.qr.generate;


import com.google.zxing.BarcodeFormat;
import com.google.zxing.MultiFormatWriter;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import org.tavall.couriers.api.console.Log;
import org.tavall.couriers.api.qr.cache.QRCodeCache;
import org.tavall.couriers.api.qr.enums.QRState;
import org.tavall.couriers.api.qr.enums.QRType;
import org.tavall.couriers.api.qr.metadata.QRMetaData;
import org.tavall.couriers.api.utils.uuid.GenerateUUID;

import java.nio.file.Path;
import java.time.Instant;

public class QRCodeGenerator {

    String GENERATION_FILE_PATH = System.getProperty("user.dir");
    Path outDir = Path.of(GENERATION_FILE_PATH);
    String IMAGE_FORMAT = "png";
    BarcodeFormat QR_CODE_FORMAT = BarcodeFormat.QR_CODE;
    int QR_WIDTH = 300;
    int QR_HEIGHT = 300;


    /**
     * Generates a QR Code for a UUID and caches the result.
     * Runs asynchronously in a Virtual Thread scope.
     */
    public QRMetaData createQRCodeWithUUID(GenerateUUID newGeneratedUUID, String imageFormat, BarcodeFormat barcodeFormat, int qrWidth, int qrHeight) throws Exception {

        // We start the Scope.
        // passing 'null' as initialData because we are about to CREATE the data.
        return QRCodeCache.runAsync(null, () -> {

            QRType type = QRType.UUID;
            String qrUUID;

            // Handle the UUID generation logic
            if (newGeneratedUUID == null) {
                GenerateUUID generateUUID = new GenerateUUID();
                qrUUID = generateUUID.getUUID().toString();
            } else {
                // Assuming GenerateUUID has a meaningful toString or getter if passed in
                qrUUID = newGeneratedUUID.getUUID().toString();
            }

            Log.info("Generating QR Matrix for UUID: " + qrUUID);

            // 1. Heavy Lifting (Matrix Encoding)
            BitMatrix matrix = new MultiFormatWriter().encode(qrUUID, barcodeFormat, qrWidth, qrHeight);
            Path outputFile = outDir.resolve("qr-" + qrUUID + "." + imageFormat);

            // 2. Blocking I/O (File Write)
            MatrixToImageWriter.writeToPath(matrix, imageFormat, outputFile);

            // 3. Build Metadata
            // Note: Passed 'newGeneratedUUID' assuming it's the object wrapper needed
            QRMetaData metaData = new QRMetaData(type, newGeneratedUUID, null, Instant.now(), QRState.ACTIVE);

            // 4. Register to Cache (The Scope is Active Here)
            QRCodeCache.get().registerQRCode(metaData);

            Log.success("QR Code Generated & Cached: " + outputFile.toString());

            return metaData;
        });
    }

    /**
     * Generates a QR Code for custom data string and caches the result.
     */
    public QRMetaData createQRCodeWithData(String data, String imageFormat, BarcodeFormat barcodeFormat, int qrWidth, int qrHeight) throws Exception {

        return QRCodeCache.runAsync(null, () -> {
            QRType type = QRType.CUSTOM;

            Log.info("Generating QR Matrix for Custom Data");

            // 1. Encode
            BitMatrix matrix = new MultiFormatWriter().encode(data, barcodeFormat, qrWidth, qrHeight);
            Path outputFile = outDir.resolve("qr-custom-" + data.hashCode() + "." + imageFormat); // Sanitize filename using hash

            // 2. Write
            MatrixToImageWriter.writeToPath(matrix, imageFormat, outputFile);

            // 3. Build Metadata
            QRMetaData metaData = new QRMetaData(type, null, data, Instant.now(), QRState.ACTIVE);

            // 4. Cache
            QRCodeCache.get().registerQRCode(metaData);

            Log.success("Custom QR Code Generated & Cached at: " + outputFile.toString());

            return metaData;
        });
    }

    //Default implementation of createQRCodeWithUUID method, class fields are set to default values
    public QRMetaData createQRCodeWithUUID() throws Exception {

        return createQRCodeWithUUID(new GenerateUUID(), IMAGE_FORMAT, QR_CODE_FORMAT, QR_WIDTH, QR_HEIGHT);
    }
    // custom path only
    public QRMetaData createQRCodeWithUUID(String path) throws Exception {

        return createQRCodeWithUUID(new GenerateUUID(), IMAGE_FORMAT, QR_CODE_FORMAT, QR_WIDTH, QR_HEIGHT);
    }

    //custom height and width
    public QRMetaData createQRCodeWithUUID(int width, int height) throws Exception {

        return createQRCodeWithUUID(new GenerateUUID(), IMAGE_FORMAT, QR_CODE_FORMAT, width, height);
    }
    //qr with custom data, default data from class fields
    public QRMetaData createQRCodeWithData(String data) throws Exception {

        return createQRCodeWithData(data, IMAGE_FORMAT, QR_CODE_FORMAT, QR_WIDTH, QR_HEIGHT);
    }

    public QRMetaData createQRCodeWithData(String data, int width, int height) throws Exception {

        return createQRCodeWithData(data, IMAGE_FORMAT, QR_CODE_FORMAT, width, height);
    }




}