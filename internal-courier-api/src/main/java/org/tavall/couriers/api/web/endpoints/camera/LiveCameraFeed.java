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
                
                <div id="ai-overlay" style="position: absolute; bottom: 20px; left: 20px; right: 20px;
                                            font-family: 'Courier New', monospace; font-weight: bold; 
                                            background: rgba(0, 0, 0, 0.85); padding: 15px; border-radius: 8px;
                                            border-left: 5px solid #444; backdrop-filter: blur(4px);">
                    <div id="status-line" style="color: #aaa; margin-bottom: 5px;">SYSTEM STATUS: OFFLINE</div>
                    <div id="data-display" style="color: #fff; font-size: 14px; line-height: 1.4;">Waiting for stream...</div>
                </div>
                
                <canvas id="frame-buffer" style="display:none;"></canvas>
            </div>

            <script>
                (function() {
                    const video = document.getElementById('live-feed');
                    const statusLine = document.getElementById('status-line');
                    const dataDisplay = document.getElementById('data-display');
                    const overlay = document.getElementById('ai-overlay');
                    
                    let consecutiveMisses = 0;

                    async function startStream() {
                        try {
                            const constraints = %s 
                                ? { video: { cursor: "always" }, audio: false }
                                : { video: { facingMode: "environment", width: { ideal: 1920 } }, audio: false };

                            const stream = await navigator.mediaDevices.%s(constraints);
                            video.srcObject = stream;
                            startGeminiLoop();
                        } catch (err) {
                            statusLine.innerText = "ERROR: CAMERA ACCESS DENIED";
                            statusLine.style.color = "red";
                        }
                    }

                    function startGeminiLoop() {
                        const loop = () => {
                            if (video.readyState !== video.HAVE_ENOUGH_DATA) {
                                setTimeout(loop, 500); return;
                            }

                            // Dynamic Polling: Slow down if we keep seeing nothing
                            const isIdle = consecutiveMisses >= 5;
                            const interval = isIdle ? 4000 : 1000; 

                            if (isIdle) {
                                statusLine.innerText = "STATUS: LOW POWER SCANNING...";
                                statusLine.style.color = "#888";
                                overlay.style.borderLeftColor = "#888";
                            } else {
                                statusLine.innerText = "STATUS: ANALYZING...";
                                statusLine.style.color = "yellow";
                                overlay.style.borderLeftColor = "yellow";
                            }

                            window.getCurrentFrameBlob(blob => {
                                const formData = new FormData();
                                formData.append('image', blob, 'frame.png');

                                fetch('%s', { method: 'POST', body: formData })
                                .then(r => r.json())
                                .then(data => {
                                    if (data.status === "SEARCHING" || data.status === "ERROR") {
                                        consecutiveMisses++;
                                        if (!isIdle) dataDisplay.innerHTML = "No package detected.";
                                    } else {
                                        // FOUND TARGET
                                        consecutiveMisses = 0;
                                        statusLine.innerText = "TARGET ACQUIRED: " + data.status;
                                        statusLine.style.color = "#0f0"; // Green
                                        overlay.style.borderLeftColor = "#0f0";
                                        
                                        // Render the rich data
                                        dataDisplay.innerHTML = `
                                            <span style="color: cyan">UUID:</span> ${data.uuid || 'N/A'}<br>
                                            <span style="color: cyan">TRK:</span>  ${data.trackingNumber || 'N/A'}<br>
                                            <span style="color: cyan">TO:</span>   ${data.name || ''}<br>
                                            <span style="color: #aaa">${data.address || ''}</span><br>
                                            <span style="color: orange">NOTE:</span> ${data.notes || 'OK'}
                                        `;
                                    }
                                    setTimeout(loop, interval);
                                })
                                .catch(e => {
                                    console.error(e);
                                    setTimeout(loop, 5000);
                                });
                            });
                        };
                        loop();
                    }

                    startStream();

                    window.getCurrentFrameBlob = function(callback) {
                         const canvas = document.getElementById('frame-buffer');
                         canvas.width = video.videoWidth; 
                         canvas.height = video.videoHeight;
                         canvas.getContext('2d').drawImage(video, 0, 0);
                         canvas.toBlob(callback, 'image/png'); 
                    };
                })();
            </script>
        """.formatted(useScreenCapture, captureMethod, streamEndpoint.endpoint());
    }

}