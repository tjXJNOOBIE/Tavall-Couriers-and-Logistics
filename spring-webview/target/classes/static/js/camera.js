(async function() {
    const endpoints = window.APP && window.APP.endpoints ? window.APP.endpoints : null;
    if (!endpoints || !endpoints.configHandshake) {
        throw new Error("Missing endpoint: configHandshake");
    }

    // --- 1. THE CONFIG HANDSHAKE ---
    const CONFIG_ENDPOINT = endpoints.configHandshake;

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
    const toggleBtn = document.getElementById('btn-toggle-source');
    const scanRoot = document.querySelector('[data-scan-mode]');
    const scanMode = scanRoot ? scanRoot.getAttribute('data-scan-mode') : null;
    const scanFrame = document.querySelector('.scan-frame');
    const stateBadge = document.getElementById('scan-state-badge');

    if (!video) {
        return;
    }

    let currentStream = null;
    let isScreenShare = false;
    const LOOP_INTERVAL_MS = 1000;
    const STATE_CLASSES = ['is-searching', 'is-analyzing', 'is-found', 'is-error'];

    // --- STREAM MANAGEMENT ---

    async function startCamera() {
        try {
            stopCurrentStream();
            const constraints = {
                video: {
                    facingMode: "environment",
                    width: { ideal: 1920 },
                    height: { ideal: 1080 },
                    frameRate: { ideal: 30, max: 60 }
                },
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
                video: {
                    cursor: "always",
                    width: { ideal: 1920 },
                    height: { ideal: 1080 },
                    frameRate: { ideal: 30, max: 60 }
                },
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
                setTimeout(loop, 500);
                return;
            }

            window.getCurrentFrameBlob(blob => {
                setCameraState('ANALYZING');
                const formData = new FormData();
                formData.append('image', blob, 'frame.png');
                if (scanMode) {
                    formData.append('scanMode', scanMode);
                }

                // USING THE SERVER-PROVIDED ENDPOINT
                fetch(API_STREAM_FRAME, { method: 'POST', body: formData })
                    .then(r => {
                        if (!r.ok) {
                            throw new Error("Frame upload failed");
                        }
                        return r.json();
                    })
                    .then(payload => {
                        if (payload && payload.cameraState) {
                            setCameraState(payload.cameraState, payload.notes, payload.geminiResponseState);
                        } else {
                            setCameraState('ANALYZING', null, null);
                        }
                        setTimeout(loop, LOOP_INTERVAL_MS);
                    })
                    .catch(e => {
                        console.error(e);
                        setCameraState('ERROR', 'Frame upload failed');
                        setTimeout(loop, 5000);
                    });
            });
        };
        loop();
    }

    function handleError(msg, err) {
        console.error(err);
        setCameraState('ERROR', msg);
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
    setCameraState('SEARCHING');
    startCamera();

    function setCameraState(state, note, geminiState) {
        if (!scanFrame || !stateBadge) {
            return;
        }
        const normalized = (state || 'SEARCHING').toUpperCase();
        const className = `is-${normalized.toLowerCase()}`;
        scanFrame.classList.remove(...STATE_CLASSES);
        scanFrame.classList.add(className);
        stateBadge.textContent = normalized;
        if (geminiState) {
            stateBadge.dataset.geminiState = geminiState;
        } else {
            delete stateBadge.dataset.geminiState;
        }
        if (note) {
            const stateNote = geminiState ? `Gemini: ${geminiState}` : null;
            stateBadge.title = [note, stateNote].filter(Boolean).join(" | ");
        } else {
            if (geminiState) {
                stateBadge.title = `Gemini: ${geminiState}`;
            } else {
                stateBadge.removeAttribute('title');
            }
        }

        if (normalized === 'FOUND') {
            triggerPulse();
        }
    }

    function triggerPulse() {
        if (!scanFrame) {
            return;
        }
        scanFrame.classList.remove('pulse');
        void scanFrame.offsetWidth;
        scanFrame.classList.add('pulse');
    }

})();
