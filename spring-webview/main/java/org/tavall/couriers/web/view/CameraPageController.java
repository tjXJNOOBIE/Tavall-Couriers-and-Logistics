package org.tavall.couriers.web.view;

import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.tavall.couriers.api.web.endpoints.CameraFeedEndpoints;
import org.tavall.couriers.api.web.endpoints.camera.LiveCameraFeed;

@RestController
public class CameraPageController {


    @GetMapping("/internal/view/camera")
    public ResponseEntity<String> getCameraPage() {

        // Render the Camera Component, pointing it to our Stream Endpoint
        String cameraHtml = LiveCameraFeed.render(CameraFeedEndpoints.STREAM_FRAME, true);
        // Wrap it in a basic shell for the browser
        String fullPage = """
            <!DOCTYPE html>
            <html lang="en">
            <head>
                <meta charset="UTF-8">
                <meta name="viewport" content="width=device-width, initial-scale=1.0">
                <title>Tavall Internal Scanner</title>
                <style>
                    body { background-color: #111; color: #eee; font-family: sans-serif; display: flex; justify-content: center; align-items: center; height: 100vh; margin: 0; }
                </style>
            </head>
            <body>
                %s
            </body>
            </html>
        """.formatted(cameraHtml);

        return ResponseEntity.ok()
                .contentType(MediaType.TEXT_HTML)
                .body(fullPage);
    }

}