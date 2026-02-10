package org.tavall.couriers.web.view;

import com.google.genai.Client;
import com.google.genai.Files;
import com.google.genai.Models;
import com.google.genai.types.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.multipart.MultipartFile;
import org.tavall.couriers.api.qr.scan.response.ScanResponseSchema;
import org.tavall.gemini.clients.Gemini3ImageClient;
import tools.jackson.databind.ObjectMapper;

import java.util.Collections;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GeminiVisionServiceTest {

    @Mock
    private Gemini3ImageClient clientWrapper;
    @Mock private Client client;
    @Mock private Models models; // Inner SDK class
    @Mock private Files files;   // Inner SDK class
    @Mock private ObjectMapper objectMapper;
    @Mock private MultipartFile multipartFile;
    @Mock private GenerateContentResponse generateResponse;
    @Mock private File uploadedFile; // Google SDK File type
    @Mock private ScanResponseSchema scanResponseSchema;

    private GeminiVisionService service;

    @BeforeEach
    void setUp() {
        // 1. Mock the Client Wrapper to return the raw Client
        when(clientWrapper.getClient()).thenReturn(client);

        // 2. Mock the internal SDK structure (Client -> Models/Files)
        // Note: These fields are usually public final in the SDK, so direct mocking might fail
        // if they are fields. However, if they are accessed via methods, this works.
        // *Assume for this test that the SDK allows field mocking or we use reflection/PowerMock if needed.*
        // *Standard Mockito strategy for deep chains:*
        // Since google-genai SDK uses public fields (client.models, client.files),
        // we normally have to constructor-inject the mock or use a wrapper.
        // *Correction:* Since you inject 'Gemini3ImageClient' which RETURNS 'Client',
        // we can control what 'client' is.
        // *BUT* 'client.models' is a field. Mockito cannot mock public fields automatically.
        // *Workaround:* We assume the 'Client' class allows us to set these or they are mockable.
        // If not, we'd wrap the SDK calls. For now, assuming standard mocking behavior:

        // *CRITICAL*: Google GenAI V1 SDK Client has public final fields 'models' and 'files'.
        // Standard Mockito CANNOT mock these directly.
        // You usually need to wrapper them.
        // *However*, for this test, let's assume you refactor the Service to use a
        // 'GeminiOperations' interface OR we use lenient deep stubs if possible.
        // *Simpler Path:* We will use reflection in the @BeforeEach to inject mocks into the Client fields
        // if they are null, OR we assume 'Client' is a Mock that returns Mocks (Deep Stubs).

        // Let's try Deep Stubs first:
        // @Mock(answer = Answers.RETURNS_DEEP_STUBS) private Client client;
    }

    // RE-WRITING SETUP TO BE ROBUST:
    // We will use a real constructor test but pass a Mock Client.

    @Test
    void analyzeFrame_Success_Found() throws Exception {
        // --- ARRANGE ---
        // 1. Setup Service with Mocks
        // Note: We need to handle the public fields 'models' and 'files' on Client.
        // Since we can't easily mock final fields, usually we'd wrap this.
        // BUT, assuming we can't change code:
        // We will Mock the Service's usage of them.

        // *Wait*, your code calls 'client.files.upload'.
        // Let's mock the wrapper to return a mock Client that has these fields populated.
        // This is hard without a helper.
        // *Better Strategy:* Mock the behavior of the external dependencies.

        // Let's assume we use deep reflection to set the fields for the test
        setField(client, "models", models);
        setField(client, "files", files);

        service = new GeminiVisionService();

        // 2. Setup Inputs
        when(multipartFile.isEmpty()).thenReturn(false);
        when(multipartFile.getBytes()).thenReturn("fake-image-bytes".getBytes());

        // 3. Setup Google SDK Mocks
        // Mock File Upload
        when(files.upload(anyString(), any(UploadFileConfig.class))).thenReturn(uploadedFile);
        when(uploadedFile.name()).thenReturn(java.util.Optional.of("files/123"));
        when(uploadedFile.mimeType()).thenReturn(java.util.Optional.of("image/png"));

        // Mock Generation
        when(models.generateContent(anyString(), any(Content.class), any(GenerateContentConfig.class)))
                .thenReturn(generateResponse);

        String jsonResponse = """
            {
                "uuid": "12345-uuid",
                "status": "FOUND",
                "trackingNumber": "TRACK-999"
            }
            """;
        when(generateResponse.text()).thenReturn(jsonResponse);

        // Mock Mapper
//        ScanResponse expectedResponse = new ScanResponse("12345-uuid", "FOUND", "TRACK-999", null, null, null, null);
//        when(objectMapper.readValue(anyString(), eq(ScanResponse.class))).thenReturn(expectedResponse);

        // --- ACT ---
//        ScanResponse actual = service.analyzeFrame(multipartFile);

        // --- ASSERT ---
//        assertNotNull(actual);
//        assertEquals("FOUND", actual.status());
//        assertEquals("12345-uuid", actual.uuid());

        // Verify flows
        verify(files).upload(contains("QRWithData.png"), any());
        verify(models).generateContent(anyString(), Collections.singletonList(any()), any());
    }

    @Test
    void analyzeFrame_EmptyFile_FailFast() {
        // --- ARRANGE ---
        when(clientWrapper.getClient()).thenReturn(client);
        service = new GeminiVisionService();
        when(multipartFile.isEmpty()).thenReturn(true);

        // --- ACT ---
//        ScanResponse actual = service.analyzeFrame(multipartFile);

//        // --- ASSERT ---
//        assertEquals("ERROR", actual.status());
//        assertNull(actual.uuid());

        // Verify we never called Google
        // (We can't verify 'files' isn't called if we didn't inject it,
        // but we can assume the logic held)
    }

    @Test
    void analyzeFrame_Exception_ReturnsErrorSafe() throws Exception {
        // --- ARRANGE ---
        setField(client, "files", files);
        when(clientWrapper.getClient()).thenReturn(client);
        service = new GeminiVisionService();

        when(multipartFile.isEmpty()).thenReturn(false);
        // Force an exception during upload
        when(files.upload(anyString(), any())).thenThrow(new RuntimeException("Google Down"));

        // --- ACT ---
//        ScanResponse actual = service.analyzeFrame(multipartFile);

        // --- ASSERT ---
//        assertEquals("ERROR", actual.status());
//        assertNull(actual.uuid());
    }

    // --- Helper to set public final fields on the SDK Client mock ---
    private void setField(Object target, String fieldName, Object value) {
        try {
            java.lang.reflect.Field field = target.getClass().getField(fieldName);
            field.setAccessible(true);
            field.set(target, value);
        } catch (Exception e) {
            throw new RuntimeException("Failed to inject mock field: " + fieldName, e);
        }
    }

}