package org.tavall.couriers.web.view;


import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.tavall.couriers.api.web.endpoints.camera.metadata.ScanResponse;
import org.tavall.couriers.api.web.endpoints.camera.state.LiveCameraState;
import org.tavall.couriers.api.intake.driver.scanner.ai.schemas.ScanResponseSchema;
import org.tavall.gemini.clients.Gemini3ImageClient;
import tools.jackson.databind.ObjectMapper;

import java.io.File;
import java.nio.file.Files;

import static org.junit.jupiter.api.Assertions.*;

public class GeminiVisonIntergrationTest {

    private GeminiVisionService service;
    private ScanResponseSchema responseSchema = new ScanResponseSchema();
    @BeforeEach
    void setUp() {
        // 1. Validate Environment
        String apiKey = System.getenv("GEMINI_API_KEY");
        if (apiKey == null || apiKey.isBlank()) {
            throw new RuntimeException("SKIPPING TEST: GEMINI_API_KEY not found in env variables.");
        }

        // 2. Validate Test File Exists
        // Your code uses: System.getProperty("user.dir") + "QRWtihData.png"
        // We verify this exists so the test fails meaningfully if it's missing.
        File testImage = new File(System.getProperty("user.dir"), "QRWithData.png");
        if (!testImage.exists()) {
            throw new RuntimeException("SKIPPING TEST: QRWtihData.png not found at " + testImage.getAbsolutePath());
        }

        // 3. Instantiate REAL Objects (No Mocks)
        Gemini3ImageClient realClientWrapper = new Gemini3ImageClient();
        ObjectMapper realMapper = new ObjectMapper();
        // 4. Create the Service with Real Dependencies
        service = new GeminiVisionService();
    }

    @Test
    void testRealInlineScanWithSchema() throws Exception {
        System.out.println("--- STARTING GEMINI 3 FLASH VISION TEST ---");

        // --- 1. PREPARE REAL DATA ---
        // Locate the test asset on disk
        File imageFile = new File("QRWithData.png");
        if (!imageFile.exists()) {
            // Fallback for different IDE working directories
            imageFile = new File(System.getProperty("user.dir") + "/QRWithData.png");
        }

        // Fail strictly if asset is missing
        if (!imageFile.exists()) {
            fail("Test asset 'QRWtihData.png' not found. Cannot run integration test.");
        }

        System.out.println("Loading Image: " + imageFile.getAbsolutePath());
        byte[] imageBytes = Files.readAllBytes(imageFile.toPath());

        // --- 2. EXECUTE (The Real Call) ---
        long startTime = System.currentTimeMillis();
        ScanResponse response = service.analyzeFrame(imageBytes);
        long duration = System.currentTimeMillis() - startTime;

        // --- 3. VERIFY (The Strict Contract) ---
        System.out.println("--- GEMINI RESPONSE (" + duration + "ms) ---");
        System.out.println(response);

        assertAll("ScanResponse Data Integrity",
                // 1. State Machine Check (Enum)
                () -> assertNotNull(response.cameraState(), "Camera State cannot be null"),
                () -> assertEquals(LiveCameraState.FOUND, response.cameraState(), "Expected state to be FOUND for a valid image"),

                // 2. UUID Check (The Anchor)
                //TODO: Re-add UUID check when we create a QR with a UUID
//                () -> assertNotNull(response.uuid(), "UUID must be extracted"),
//                () -> assertEquals("af36b132-8e10-4f51-a9d0-c5b73d2a0e2d", response.uuid(), "UUID mismatch - critical failure"),

                // 3. OCR Data Checks
                () -> assertEquals("TRK-GOOGLE-9988", response.trackingNumber(), "Tracking Number mismatch"),
                () -> assertEquals("Google Gemini", response.name(), "Recipient Name mismatch"),
                () -> assertEquals("(555) 019-2834", response.phoneNumber(), "Phone Number mismatch"),

                // 4. Address Partial Match (Allows for minor formatting differences by AI)
                () -> assertTrue(response.address() != null && response.address().contains("1600 Amphitheatre"),
                        "Address should contain '1600 Amphitheatre'. Got: " + response.address()),

                // 5. Notes/Context
                () -> assertTrue(response.notes() != null && response.notes().toLowerCase().contains("do not bend"),
                        "Notes should contain warning 'Do not bend'. Got: " + response.notes())
        );

        System.out.println("--- TEST PASSED: SCHEMA & DATA VALIDATED ---");
    }
@Test
void testRealInlineScan() throws Exception {
    // 1. Get Bytes from Disk
    java.io.File realFile = new java.io.File("QRWithData.png");
    if (!realFile.exists()) {
        realFile = new java.io.File(System.getProperty("user.dir") + "/QRWtihData.png");
    }
    byte[] fileBytes = Files.readAllBytes(realFile.toPath());

    // 2. Call Service Directly
    ScanResponse response = service.analyzeFrame(fileBytes);

    // 3. Verify
    System.out.println("Status: " + response.cameraState());

}
}