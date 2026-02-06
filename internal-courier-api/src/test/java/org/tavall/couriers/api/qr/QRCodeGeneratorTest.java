package org.tavall.couriers.api.qr;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.tavall.couriers.api.qr.enums.QRState;
import org.tavall.couriers.api.qr.enums.QRType;
import org.tavall.couriers.api.qr.generate.QRCodeGenerator;
import org.tavall.couriers.api.qr.metadata.QRMetaData;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Fail.fail;
import static org.junit.jupiter.api.Assertions.*;

class QRCodeGeneratorTest {

    private QRCodeGenerator generator;

    // Keep test output in a stable place under the repo, not temp dirs.
    private Path testOutDir;
    private final List<Path> createdFiles = new ArrayList<>();


    @BeforeEach
    void setup() throws Exception {

        generator = new QRCodeGenerator();
        testOutDir = Path.of(System.getProperty("user.dir"), "build", "qr-test-output");
        Files.createDirectories(testOutDir);
    }


    @AfterEach
    void cleanup() throws Exception {
        // Delete files we created (best-effort cleanup)
        for (Path p : createdFiles) {
            try {
                Files.deleteIfExists(p);
            } catch (Exception ignored) {
            }
        }
        createdFiles.clear();
    }



    @Test
    void createQRCodeWithUUIDTest() {

        assertDoesNotThrow(() -> {
            QRMetaData meta = generator.createQRCodeWithUUID();

            assertNotNull(meta);
            assertEquals(QRType.UUID, meta.getQrType());
            assertEquals(QRState.ACTIVE, meta.getQrState());
            assertNotNull(meta.getUuid());
            assertNull(meta.getQRData());
            assertNotNull(meta.getCreatedAt());
        });
    }


    @Test
    void createQRCodeWithDataStringTest() {

        assertDoesNotThrow(() -> {
            String data = "custom-data";

            QRMetaData meta = generator.createQRCodeWithData(data);

            assertNotNull(meta);
            assertEquals(QRType.CUSTOM, meta.getQrType());
            assertEquals(QRState.ACTIVE, meta.getQrState());
            assertNull(meta.getUuid());
            assertEquals(data, meta.getQRData());
            assertNotNull(meta.getCreatedAt());
        });
    }

}