(async function() {
    const endpoints = window.APP && window.APP.endpoints ? window.APP.endpoints : null;
    if (!endpoints || !endpoints.configHandshake) {
        throw new Error("Missing endpoint: configHandshake");
    }

    // --- 1. THE CONFIG HANDSHAKE ---
    const CONFIG_ENDPOINT = endpoints.configHandshake;

    let config = null;
    let API_STREAM_FRAME = null;
    const scanRoot = document.querySelector('[data-camera-mode-key]');
    const cameraModeKey = scanRoot ? scanRoot.getAttribute('data-camera-mode-key') : null;
    const routeId = scanRoot ? scanRoot.getAttribute('data-route-id') : null;
    let loadedCameraOptions = null;
    let cameraType = 'INTAKE';
    let scanModeValue = null;

    try {
        const response = await fetch(CONFIG_ENDPOINT);
        if (!response.ok) throw new Error("Config handshake failed");
        config = await response.json();

        const cameraConfig = config.cameraConfig || {};
        window.APP.cameraConfig = cameraConfig;
        const cameraModes = cameraConfig.modes || {};
        const defaultModeKey = cameraConfig.defaultModeKey;
        const fallbackKeys = cameraModes ? Object.keys(cameraModes) : [];
        const resolvedKey = cameraModeKey || defaultModeKey || (fallbackKeys.length ? fallbackKeys[0] : null);
        loadedCameraOptions = resolvedKey ? cameraModes[resolvedKey] : null;
        if (!loadedCameraOptions && fallbackKeys.length) {
            loadedCameraOptions = cameraModes[fallbackKeys[0]];
        }
        cameraType = loadedCameraOptions?.type || 'INTAKE';
        scanModeValue = loadedCameraOptions?.mode || null;

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
    const scanFrame = document.querySelector('.scan-frame');
    const stateBadge = document.getElementById('scan-state-badge');
    const frameBuffer = document.getElementById('frame-buffer');
    const frameCtx = frameBuffer ? frameBuffer.getContext('2d', { willReadFrequently: true }) : null;
    const dataOverlay = document.querySelector('.scan-data-overlay');
    const dataFlash = dataOverlay ? dataOverlay.querySelector('.scan-data-flash') : null;

    if (!video) {
        return;
    }

    let currentStream = null;
    let isScreenShare = false;
    const LOOP_INTERVAL_MS = 900;
    const BUSY_INTERVAL_MS = 1600;
    const MIN_UPLOAD_GAP_MS = 850;
    const COOLDOWN_MS = 5000;
    const STATE_CLASSES = ['is-searching', 'is-analyzing', 'is-found', 'is-error'];
    let lastUploadAt = 0;
    let lastGeminiState = null;
    let lastCameraState = null;
    let cooldownUntil = 0;
    let lastFoundUuid = null;
    let lastFlashKey = null;
    let lastFlashAt = 0;
    let flashTimer = null;

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

            const now = Date.now();
            if (cooldownUntil && now < cooldownUntil) {
                setTimeout(loop, LOOP_INTERVAL_MS);
                return;
            }

            if (now - lastUploadAt < MIN_UPLOAD_GAP_MS) {
                setTimeout(loop, LOOP_INTERVAL_MS);
                return;
            }

            window.getCurrentFrameBlob(blob => {
                if (!blob) {
                    setCameraState('ERROR', 'Frame capture failed');
                    setTimeout(loop, 500);
                    return;
                }
                lastUploadAt = Date.now();
                const formData = new FormData();
                formData.append('image', blob, 'frame.png');
                if (scanModeValue) {
                    formData.append('scanMode', scanModeValue);
                }
                if (routeId) {
                    formData.append('routeId', routeId);
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
                            lastCameraState = payload.cameraState;
                            lastGeminiState = payload.geminiResponseState || null;
                            setCameraState(payload.cameraState, payload.notes, payload.geminiResponseState);
                        } else {
                            const fallbackState = lastCameraState || 'SEARCHING';
                            lastCameraState = fallbackState;
                            lastGeminiState = null;
                            setCameraState(fallbackState, null, null);
                        }

                        if (payload && payload.cameraState === "FOUND") {
                            const now = Date.now();
                            if (cameraType === "QR_SCAN" && payload.uuid) {
                                const isEmbedded = window.parent && window.parent !== window;
                                const shouldNotify = payload.uuid !== lastFoundUuid || now >= cooldownUntil;
                                lastFoundUuid = payload.uuid;
                                cooldownUntil = now + COOLDOWN_MS;
                                if (shouldNotify) {
                                    if (isEmbedded) {
                                        window.parent.postMessage({ type: "driverScanFound", uuid: payload.uuid }, window.location.origin);
                                    } else {
                                        window.location.href = `/dashboard/driver/state?uuid=${encodeURIComponent(payload.uuid)}`;
                                    }
                                    return;
                                }
                            } else if (cameraType === "ROUTE") {
                                const isEmbedded = window.parent && window.parent !== window;
                                if (isEmbedded) {
                                    window.parent.postMessage({ type: "routeScanResult", routeId, payload }, window.location.origin);
                                }
                                cooldownUntil = now + COOLDOWN_MS;
                            } else {
                                cooldownUntil = now + COOLDOWN_MS;
                            }
                            handleLiveFlash(payload);
                        }

                        const delay = (lastGeminiState === "RESPONDING" || lastCameraState === "ANALYZING")
                            ? BUSY_INTERVAL_MS
                            : LOOP_INTERVAL_MS;
                        setTimeout(loop, delay);
                    })
                    .catch(e => {
                        console.error(e);
                        lastCameraState = "ERROR";
                        lastGeminiState = "ERROR";
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
        if (!frameBuffer || !frameCtx || !video.videoWidth || !video.videoHeight) {
            callback(null);
            return;
        }
        frameBuffer.width = video.videoWidth;
        frameBuffer.height = video.videoHeight;
        frameCtx.drawImage(video, 0, 0);
        frameBuffer.toBlob(callback, 'image/png');
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

    function handleLiveFlash(payload) {
        if (!dataOverlay || !dataFlash || !payload) {
            return;
        }
        const tone = payload.cameraState === "ERROR" ? "error" : "success";
        dataOverlay.classList.toggle('error', tone === "error");

        const lines = [];
        if (tone === "error") {
            const note = payload.notes || "Verification failed";
            lines.push(note);
        } else {
            if (payload.name) {
                lines.push(payload.name);
            }
            const addressParts = [payload.address, payload.city, payload.state, payload.zipCode]
                .filter(Boolean)
                .map(part => String(part).trim())
                .filter(part => part.length);
            if (addressParts.length) {
                lines.push(addressParts.join(', '));
            }
            if (payload.trackingNumber) {
                lines.push(`Tracking: ${payload.trackingNumber}`);
            }
            if (!lines.length) {
                lines.push("Address verified");
            }
        }

        const key = payload.uuid || payload.trackingNumber || payload.notes || Math.random().toString();
        const now = Date.now();
        if (key && key === lastFlashKey && now - lastFlashAt < 1400) {
            return;
        }
        lastFlashKey = key;
        lastFlashAt = now;

        dataFlash.innerHTML = '';
        lines.forEach((line) => {
            const div = document.createElement('div');
            div.className = 'scan-data-line';
            div.textContent = line;
            dataFlash.appendChild(div);
        });
        dataOverlay.classList.add('show');
        if (flashTimer) {
            clearTimeout(flashTimer);
        }
        flashTimer = setTimeout(() => {
            dataOverlay.classList.remove('show');
        }, tone === "error" ? 2000 : 1400);
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
