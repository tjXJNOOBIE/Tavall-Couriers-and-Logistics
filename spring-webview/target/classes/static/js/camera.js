(async function() {
    // --- 1. THE CONFIG HANDSHAKE ---
    // The only hardcoded path we allow. Everything else comes from Java.
    const CONFIG_ENDPOINT = '/internal/api/v1/config/handshake';

    let config = null;
    let API_STREAM_FRAME = null;

    try {
        const response = await fetch(CONFIG_ENDPOINT);
        if (!response.ok) throw new Error("Config handshake failed");
        config = await response.json();

        // Hydrate our constants from the server
        API_STREAM_FRAME = config.endpoints.streamFrame;
        console.log("System Config Loaded:", config);

    } catch (e) {
        console.error("FATAL: Could not load system config", e);
        document.body.innerHTML = `
            <div style="color:red; text-align:center; margin-top:20%">
                <h1>SYSTEM OFFLINE</h1>
                <p>Could not handshake with Control Server.</p>
            </div>`;
        return; // Kill the app here if we can't talk to Java
    }

    // --- 2. YOUR ORIGINAL LOGIC (Unchanged, just using the new var) ---

    const video = document.getElementById('live-feed');
    const statusLine = document.getElementById('status-line');
    const dataDisplay = document.getElementById('data-display');
    const overlay = document.getElementById('ai-overlay');
    const toggleBtn = document.getElementById('btn-toggle-source');

    if (!video) {
        return;
    }

    let consecutiveMisses = 0;
    let currentStream = null;
    let isScreenShare = false;

    // --- STREAM MANAGEMENT ---

    async function startCamera() {
        try {
            stopCurrentStream();
            const constraints = {
                video: { facingMode: "environment", width: { ideal: 1920 } },
                audio: false
            };
            const stream = await navigator.mediaDevices.getUserMedia(constraints);
            handleStreamSuccess(stream, false);
        } catch (err) {
            handleError("CAMERA DENIED", err);
        }
    }

    async function startScreenShare() {
        try {
            stopCurrentStream();
            const stream = await navigator.mediaDevices.getDisplayMedia({
                video: { cursor: "always" },
                audio: false
            });
            handleStreamSuccess(stream, true);
        } catch (err) {
            console.warn("Screen share cancelled", err);
            startCamera();
        }
    }

    function handleStreamSuccess(stream, isScreen) {
        currentStream = stream;
        video.srcObject = stream;
        isScreenShare = isScreen;

        if (toggleBtn) {
            toggleBtn.innerText = isScreen ? "SWITCH TO CAMERA" : "SWITCH TO SCREEN SHARE";
        }
        if (statusLine) {
            statusLine.innerText = isScreen ? "STATUS: SCREEN CAPTURE ACTIVE" : "STATUS: CAMERA ACTIVE";
            statusLine.style.color = "#aaa";
        }
        if (overlay) {
            overlay.style.borderLeftColor = "#444";
        }

        stream.getVideoTracks()[0].onended = () => {
            if (isScreen) startCamera();
        };

        startGeminiLoop();
    }

    function stopCurrentStream() {
        if (currentStream) {
            currentStream.getTracks().forEach(track => track.stop());
        }
    }

    // --- AI LOOP ---

    let loopActive = false;
    function startGeminiLoop() {
        if (loopActive) return;
        loopActive = true;

        const loop = () => {
            if (video.readyState !== video.HAVE_ENOUGH_DATA) {
                setTimeout(loop, 500); return;
            }

            const isIdle = consecutiveMisses >= 5;
            const interval = isIdle ? 4000 : 1000;

            if (!isIdle) {
                if (statusLine) {
                    statusLine.innerText = "STATUS: ANALYZING...";
                    statusLine.style.color = "yellow";
                }
                if (overlay) {
                    overlay.style.borderLeftColor = "yellow";
                }
            }

            window.getCurrentFrameBlob(blob => {
                const formData = new FormData();
                formData.append('image', blob, 'frame.png');

                // USING THE SERVER-PROVIDED ENDPOINT
                fetch(API_STREAM_FRAME, { method: 'POST', body: formData })
                    .then(r => r.json())
                    .then(data => {
                        const state = data.cameraState || data.status;

                        if (state === "SEARCHING" || state === "ERROR" || state === "IDLE") {
                            consecutiveMisses++;
                            if (isIdle) {
                                if (statusLine) {
                                    statusLine.innerText = "STATUS: IDLE (LOW POWER)";
                                    statusLine.style.color = "#888";
                                }
                                if (overlay) {
                                    overlay.style.borderLeftColor = "#888";
                                }
                            }
                        } else {
                            consecutiveMisses = 0;
                            if (statusLine) {
                                statusLine.innerText = "TARGET ACQUIRED: " + state;
                                statusLine.style.color = "#0f0";
                            }
                            if (overlay) {
                                overlay.style.borderLeftColor = "#0f0";
                            }
                            if (dataDisplay) {
                                dataDisplay.innerHTML = `
                                <span style="color: cyan">UUID:</span> ${data.uuid || 'N/A'}<br>
                                <span style="color: cyan">TRK:</span>  ${data.trackingNumber || 'N/A'}<br>
                                <span style="color: cyan">TO:</span>   ${data.name || ''}<br>
                                <span style="color: #aaa">${data.address || ''}</span><br>
                                <span style="color: orange">NOTE:</span> ${data.notes || 'OK'}
                            `;
                            }
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

    function handleError(msg, err) {
        console.error(err);
        if (statusLine) {
            statusLine.innerText = "ERROR: " + msg;
            statusLine.style.color = "red";
        }
        if (overlay) {
            overlay.style.borderLeftColor = "red";
        }
    }

    // --- INITIALIZATION ---

    if (toggleBtn) {
        toggleBtn.addEventListener('click', () => {
            if (isScreenShare) {
                startCamera();
            } else {
                startScreenShare();
            }
        });
    }

    window.getCurrentFrameBlob = function(callback) {
        const canvas = document.getElementById('frame-buffer');
        canvas.width = video.videoWidth;
        canvas.height = video.videoHeight;
        canvas.getContext('2d').drawImage(video, 0, 0);
        canvas.toBlob(callback, 'image/png');
    };

    // Ignite (Only after config is loaded)
    startCamera();

})();
