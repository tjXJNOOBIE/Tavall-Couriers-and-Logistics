//package org.tavall.couriers.web.view;
//
//
//import org.junit.jupiter.api.BeforeEach;
//import org.junit.jupiter.api.Test;
//import org.slf4j.Logger;
//import org.slf4j.LoggerFactory;
//import org.tavall.couriers.api.cache.interfaces.ICacheKey;
//import org.tavall.couriers.api.cache.interfaces.ICacheValue;
//import org.tavall.couriers.api.cache.maps.CacheMap;
//import org.tavall.couriers.api.console.Log;
//import org.tavall.couriers.api.qr.scan.response.ScanResponseSchema;
//import org.tavall.gemini.clients.Gemini3ImageClient;
//import org.tavall.gemini.clients.response.Gemini3Response;
//import org.tavall.couriers.api.qr.scan.metadata.ScanResponse;
//import org.tavall.couriers.api.qr.scan.state.CameraState;
//import org.tavall.couriers.api.qr.scan.cache.ScanCacheService;
//import tools.jackson.databind.ObjectMapper;
//
//import java.io.File;
//import java.nio.file.Files;
//import java.util.List;
//import java.util.concurrent.CompletableFuture;
//import java.util.concurrent.CountDownLatch;
//import java.util.concurrent.TimeUnit;
//
//import static org.junit.jupiter.api.Assertions.*;
//
//public class GeminiVisonIntergrationTest {
//
//    private GeminiVisionService service;
//    private ScanResponseSchema responseSchema = new ScanResponseSchema();
//    private static final Logger log = LoggerFactory.getLogger(GeminiVisionServiceTest.class);
//    @BeforeEach
//    void setUp() {
//        // 1. Validate Environment
//        String apiKey = System.getenv("GEMINI_API_KEY");
//        if (apiKey == null || apiKey.isBlank()) {
//            throw new RuntimeException("SKIPPING TEST: GEMINI_API_KEY not found in env variables.css.");
//        }
//
//        // 2. Validate Test File Exists
//        // Your code uses: System.getProperty("user.dir") + "QRWtihData.png"
//        // We verify this exists so the test fails meaningfully if it's missing.
//        File testImage = new File(System.getProperty("user.dir"), "QRWithData.png");
//        if (!testImage.exists()) {
//            throw new RuntimeException("SKIPPING TEST: QRWtihData.png not found at " + testImage.getAbsolutePath());
//        }
//
//        // 3. Instantiate REAL Objects (No Mocks)
//        Gemini3ImageClient realClientWrapper = new Gemini3ImageClient();
//        ObjectMapper realMapper = new ObjectMapper();
//        // 4. Create the Service with Real Dependencies
//        service = new GeminiVisionService();
//    }
//
//    @Test
//    void testRealInlineScanWithSchema() throws Exception {
//        System.out.println("--- STARTING GEMINI 3 FLASH VISION TEST ---");
//
//        File imageFile = new File("QRWithData.png");
//        if (!imageFile.exists()) imageFile = new File(System.getProperty("user.dir") + "/QRWithData.png");
//
//        byte[] imageBytes = Files.readAllBytes(imageFile.toPath());
//
//        long startTime = System.currentTimeMillis();
//        // .getResponse() handles the wrapper
//        ScanResponse response = service.analyzeFrame(imageBytes).getResponse();
//        long duration = System.currentTimeMillis() - startTime;
//
//        System.out.println("--- GEMINI RESPONSE (" + duration + "ms) ---");
//        System.out.println(response);
//
//        assertAll("ScanResponse Data Integrity",
//                () -> assertNotNull(response.cameraState(), "Camera State cannot be null"),
//                () -> assertEquals(CameraState.FOUND, response.cameraState(), "Expected state to be FOUND"),
//                () -> assertEquals("TRK-GOOGLE-9988", response.trackingNumber(), "Tracking Number mismatch")
//        );
//    }
//
//    @Test
//    void analyzeFrameAsync_runsInBackground_andClonesInputBytes() throws Exception {
//        log.info("Test: Async Cloning");
//        TestableGeminiVisionService testService = new TestableGeminiVisionService();
//        byte[] input = new byte[] { 1, 2, 3 };
//
//        CompletableFuture<Gemini3Response<ScanResponse>> future = testService.analyzeFrameAsync(input);
//
//        assertTrue(testService.started.await(2, TimeUnit.SECONDS), "Async task did not start");
//        assertFalse(future.isDone(), "Future finished too early");
//
//        // Mutate input
//        input[0] = 9;
//        testService.proceed.countDown();
//
//        ScanResponse result = future.get().getResponse();
//
//        assertEquals(CameraState.FOUND, result.cameraState());
//        assertEquals(1, testService.seenBytes[0], "Byte array was not cloned!");
//    }
//
//    @Test
//    void analyzeFrameAsync_whenAnalyzeFrameThrows_mapsToErrorResponse() throws Exception {
//        log.info("Test: Async Exception Mapping");
//
//        // Create a service that explodes
//        GeminiVisionService throwingService = new GeminiVisionService() {
//            @Override
//            public Gemini3Response<ScanResponse> analyzeFrame(byte[] frameData) {
//                throw new RuntimeException("boom");
//            }
//        };
//
//        CompletableFuture<Gemini3Response<ScanResponse>> future = throwingService.analyzeFrameAsync(new byte[] { 1 });
//
//        ScanResponse result = future.get().getResponse();
//
//        // This assertion passes now because we fixed the service to return a Response, not null
//        assertNotNull(result, "Result should not be null on error");
//        assertEquals(CameraState.ERROR, result.cameraState(), "State should be ERROR");
//        assertTrue(result.notes().contains("boom"), "Notes should contain error message");
//    }
//    @Test
//    void testRealAsyncScan_WithRealImage_ReturnsActualGeminiData() throws Exception {
//        System.out.println("--- STARTING REAL ASYNC GEMINI TEST ---");
//
//        // 1. SETUP REAL DATA
//        File imageFile = new File("QRWithData.png");
//        if (!imageFile.exists()) imageFile = new File(System.getProperty("user.dir") + "/QRWithData.png");
//        byte[] realImageBytes = Files.readAllBytes(imageFile.toPath());
//
//        // 2. EXECUTE ASYNC (The "Fire" Step)
//        long fireStartTime = System.currentTimeMillis();
//
//        // This should return INSTANTLY because it's just spinning up a virtual thread
//        CompletableFuture<Gemini3Response<ScanResponse>> future = service.analyzeFrameAsync(realImageBytes);
//
//        long fireDuration = System.currentTimeMillis() - fireStartTime;
//        System.out.println("[Main Thread] 'analyzeFrameAsync' returned future in: " + fireDuration + "ms");
//
//        // ASSERTION 1: The main thread was NOT blocked by the network call
//        // A real API call takes ~2000ms. If this took < 50ms, we successfully offloaded it.
//        assertTrue(fireDuration < 100, "Main thread was blocked! Async failed.");
//        assertFalse(future.isDone(), "Future completed too fast - it should be fetching data in background.");
//
//        // 3. WAIT FOR RESULT (The "Forget" Step... eventually)
//        System.out.println("[Main Thread] Now waiting for real Gemini API response...");
//
//        // We give it 15 seconds to be safe (Gemini Vision can be slow on cold starts)
//        Gemini3Response<ScanResponse> resultWrapper = future.get(15, TimeUnit.SECONDS);
//        ScanResponse response = resultWrapper.getResponse();
//
//        assertNotNull(response, "Response should not be null");
//        assertEquals(CameraState.FOUND, response.cameraState(), "State should be FOUND");
//
//        ICacheKey<ScanCacheService> cacheKey = ScanCacheService.INSTANCE.getScanCacheKey();
//        assertTrue(ScanCacheService.INSTANCE.containsScanKey(),
//                "CacheMap missing ScanCacheService domain bucket");
//        List<ICacheValue<?>> bucket = CacheMap.INSTANCE.get(cacheKey);
//        assertNotNull(bucket, "Domain bucket should not be null");
//        boolean found = bucket.stream()
//                .anyMatch(v -> response.equals(v.getValue()));
//
//        assertTrue(found, "ScanResponse not found inside ScanCacheService bucket");
//        Log.success(bucket.size() + " items in bucket: " + bucket);
//        for (int i = 0; i < bucket.size(); i++) {
//            ICacheValue<?> value = bucket.get(i);
//            Log.success("[" + i + "] -> " + value.getValue());
//        }
//    }
//    // --- Testable Subclass for Latch Logic ---
//    private static final class TestableGeminiVisionService extends GeminiVisionService {
//        final CountDownLatch started = new CountDownLatch(1);
//        final CountDownLatch proceed = new CountDownLatch(1);
//        volatile byte[] seenBytes;
//
//        @Override
//        public Gemini3Response<ScanResponse> analyzeFrame(byte[] frameData) {
//            started.countDown();
//            try {
//                if (!proceed.await(2, TimeUnit.SECONDS)) throw new RuntimeException("Latch Timeout");
//            } catch (InterruptedException e) {
//                Thread.currentThread().interrupt();
//            }
//            this.seenBytes = frameData;
//            // Return Dummy Success
//            return new Gemini3Response<>(new ScanResponse(
//                    "uuid-test", CameraState.FOUND, "TRK", null, null, null, null, null
//            ));
//        }
//    }
////    @Test
////    void testRealAsyncIngestion_PushesToCache() throws Exception {
////        System.out.println("--- TEST: ASYNC INGESTION TO CACHE ---");
////
////        // 1. SETUP
////        File imageFile = new File("QRWithData.png");
////        if (!imageFile.exists()) imageFile = new File(System.getProperty("user.dir") + "/QRWithData.png");
////        byte[] realBytes = Files.readAllBytes(imageFile.toPath());
////
////        // Ensure Cache is empty
////        assertEquals(0, scanCache.size(), "Cache should be empty start");
////        // 2. FIRE (Main Thread)
////        long start = System.currentTimeMillis();
////        CompletableFuture<Void> pipeline = service.ingestFrameAsync(realBytes);
////        long duration = System.currentTimeMillis() - start;
////
////        // ASSERT: Main thread free instantly
////        assertTrue(duration < 100, "Main thread blocked!");
////        assertEquals(0, scanCache.size(), "Cache should still be empty (AI is thinking...)");
////
////        // 3. WAIT (Simulate time passing)
////        System.out.println("[Test] Waiting for pipeline to finish...");
////        pipeline.join(); // Block ONLY for test purposes
////
////        // 4. VERIFY SIDE EFFECT
////        assertEquals(1, scanCache.size(), "Cache should now contain the scan");
////
////        // Retrieve by the known UUID in the QR image
////        // (Note: Update this string to match whatever your QR actually contains)
//////        ScanResponse cached = scanCache.get("a");
////
//////        assertNotNull(cached, "Should retrieve object from cache");
//////        assertEquals("TRK-GOOGLE-9988", cached.trackingNumber());
////
////        System.out.println("--- TEST PASSED: Data landed in Cache ---");
////    }
//}
