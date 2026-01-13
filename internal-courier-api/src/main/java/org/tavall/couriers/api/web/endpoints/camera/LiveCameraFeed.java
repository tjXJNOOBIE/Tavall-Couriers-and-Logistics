package org.tavall.couriers.api.web.endpoints.camera;


import org.tavall.couriers.api.web.endpoints.interfaces.AppEndpoint;

public class LiveCameraFeed {


    // Overload: Default to false (Production behavior)
    public static String render(AppEndpoint streamEndpoint) {

        return render(streamEndpoint, false);
    }


    public static String render(AppEndpoint streamEndpoint, boolean useScreenCapture) {
        // Prepare the JS method name based on the boolean
        String captureMethod = useScreenCapture ? "getDisplayMedia" : "getUserMedia";

        return """
            <div id="gemini-vision-container" style="position: relative; width: 100%%; max-width: 1000px; margin: 0 auto;">
                <video id="live-feed" autoplay playsinline muted 
                       style="width: 100%%; border-radius: 12px; border: 2px solid #333; box-shadow: 0 0 20px rgba(0,255,0,0.2);">
                </video>
                
                <div id="ai-overlay" style="position: absolute; bottom: 20px; left: 20px; right: 20px;
                                            color: #aaa; font-family: monospace; font-weight: bold; 
                                            background: rgba(0,0,0,0.8); padding: 10px; border-radius: 4px;">
                    System Status: IDLE (Waiting for Stream...)
                </div>
                
                <canvas id="frame-buffer" style="display:none;"></canvas>
            </div>

            <script>
                (function() {
                    const video = document.getElementById('live-feed');
                    const status = document.getElementById('ai-overlay');
                    
                    // 1. Inject Java Variables into JS
                    const useScreen = %s; // Injected Boolean (true/false)
                    const endpoint = "%s"; // Injected URL String
                    
                    // State Tracking
                    let consecutiveMisses = 0;

                    // A. Start Camera/Screen
                    async function startStream() {
                        try {
                            const constraints = useScreen 
                                ? { video: { cursor: "always" }, audio: false }
                                : { video: { facingMode: "environment", width: { ideal: 1920 } }, audio: false };

                            // Dynamic Method Call: getDisplayMedia or getUserMedia
                            const stream = await navigator.mediaDevices.%s(constraints);
                            video.srcObject = stream;
                            
                            startGeminiLoop();
                            
                        } catch (err) {
                            status.innerText = "Error: " + err.message;
                            status.style.color = "red";
                        }
                    }

                    // B. The Gemini Loop (Variable Speed)
                    function startGeminiLoop() {
                        const loop = () => {
                            // Wait for video to be ready
                            if (video.readyState !== video.HAVE_ENOUGH_DATA) {
                                setTimeout(loop, 500); 
                                return;
                            }

                            // Power Save Logic: If 5 misses, slow down to 5s. If found, speed up to 1s.
                            const isIdle = consecutiveMisses >= 5;
                            const currentInterval = isIdle ? 5000 : 1000; 

                            if (isIdle) {
                                status.innerText = "Status: LOW POWER MODE (Scanning every 5s)";
                                status.style.color = "#777";
                            }

                            window.getCurrentFrameBlob(blob => {
                                const formData = new FormData();
                                formData.append('image', blob, 'live_frame.png');

                                if (!isIdle) {
                                    status.innerText = "Status: GEMINI ANALYZING...";
                                    status.style.color = "yellow";
                                }

                                fetch(endpoint, { method: 'POST', body: formData })
                                .then(r => r.json())
                                .then(data => {
                                    if (data.status === "SEARCHING" || data.status === "ERROR") {
                                        consecutiveMisses++;
                                    } else {
                                        // FOUND IT! Reset timeout
                                        consecutiveMisses = 0; 
                                        status.innerHTML = `UUID: ${data.uuid} <br/> ${data.description}`;
                                        status.style.color = "#00ccff";
                                    }
                                    
                                    // Schedule next run
                                    setTimeout(loop, currentInterval);
                                })
                                .catch(e => {
                                    console.error(e);
                                    setTimeout(loop, 5000); // Back off on network error
                                });
                            });
                        };
                        
                        loop();
                    }

                    startStream();

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
        """.formatted(
                useScreenCapture,       // 1. %s (boolean) - fixed variable name
                streamEndpoint.endpoint(),  // 2. "%s" (endpoint string)
                captureMethod           // 3. .%s (method name string)
        );
    }
}