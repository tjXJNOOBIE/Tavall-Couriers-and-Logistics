package org.tavall.couriers.api.web.endpoints.camera;


import org.tavall.couriers.api.web.endpoints.interfaces.AppEndpoint;

public class LiveCameraFeed {


    // Overload: Default to false (Production behavior)
    public static String render(AppEndpoint streamEndpoint) {
        return render(streamEndpoint, false);
    }

    public static String render(AppEndpoint streamEndpoint, boolean useScreenCapture) {
        String captureMethod = useScreenCapture ? "getDisplayMedia" : "getUserMedia";

        return """
            <div id="gemini-vision-container" style="position: relative; width: 100%%; max-width: 1000px; margin: 0 auto;">
                <video id="live-feed" autoplay playsinline muted 
                       style="width: 100%%; border-radius: 12px; border: 2px solid #333; box-shadow: 0 0 20px rgba(0,255,0,0.2);">
                </video>
                
                <div id="ai-overlay" style="position: absolute; bottom: 20px; left: 20px; 
                                            color: #0f0; font-family: monospace; font-weight: bold; 
                                            background: rgba(0,0,0,0.7); padding: 5px 10px; border-radius: 4px;">
                    Initializing Source: %s...
                </div>
                
                <button id="start-btn" style="display:none; position: absolute; top: 50%%; left: 50%%; transform: translate(-50%%, -50%%); padding: 20px;">
                    START SCREEN SHARE
                </button>

                <canvas id="frame-buffer" style="display:none;"></canvas>
            </div>

            <script>
                (function() {
                    const video = document.getElementById('live-feed');
                    const status = document.getElementById('ai-overlay');
                    const startBtn = document.getElementById('start-btn');
                    const useScreen = %s; 

                    async function startStream() {
                        try {
                            let stream;
                            if (useScreen) {
                                stream = await navigator.mediaDevices.getDisplayMedia({
                                    video: { cursor: "always" },
                                    audio: false
                                });
                            } else {
                                stream = await navigator.mediaDevices.getUserMedia({ 
                                    video: { facingMode: "environment", width: { ideal: 1920 } },
                                    audio: false
                                });
                            }
                            
                            video.srcObject = stream;
                            status.innerText = "System Status: ACTIVE (" + (useScreen ? "SCREEN" : "CAMERA") + ")";
                            startBtn.style.display = "none";
                            
                        } catch (err) {
                            console.error("Stream Error", err);
                            status.innerText = "Error: " + err.message;
                            status.style.color = "red";
                            
                            if (useScreen) startBtn.style.display = "block"; 
                        }
                    }

                    startStream();
                    startBtn.addEventListener('click', startStream);

                    window.getCurrentFrameBlob = function(callback) {
                         if (video.readyState === video.HAVE_ENOUGH_DATA) {
                            const canvas = document.getElementById('frame-buffer');
                            canvas.width = video.videoWidth;
                            canvas.height = video.videoHeight;
                            canvas.getContext('2d').drawImage(video, 0, 0);
                            canvas.toBlob(callback, 'image/png'); 
                        }
                    };
                })();
            </script>
        """.formatted(captureMethod, useScreenCapture);
    }
}