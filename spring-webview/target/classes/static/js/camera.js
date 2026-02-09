(async function() {
    const endpoints = window.APP && window.APP.endpoints ? window.APP.endpoints : null;
    if (!endpoints || !endpoints.configHandshake) {
        throw new Error("Missing endpoint: configHandshake");
    }

    const csrf = (() => {
        const token = document.querySelector('meta[name="_csrf"]')?.getAttribute("content") || "";
        const header = document.querySelector('meta[name="_csrf_header"]')?.getAttribute("content") || "";
        const param = document.querySelector('meta[name="_csrf_parameter"]')?.getAttribute("content") || "_csrf";
        return { token, header, param };
    })();

    function applyCsrf(headers) {
        if (csrf.header && csrf.token) {
            headers[csrf.header] = csrf.token;
        }
        return headers;
    }

    // --- 1. THE CONFIG HANDSHAKE ---
    const CONFIG_ENDPOINT = endpoints.configHandshake;

    let config = null;
    let API_STREAM_FRAME = null;
    let API_CLOSE_SESSION = null;
    const scanRoot = document.querySelector('[data-camera-mode-key]');
    const cameraModeKey = scanRoot ? scanRoot.getAttribute('data-camera-mode-key') : null;
    const routeId = scanRoot ? scanRoot.getAttribute('data-route-id') : null;
    let loadedCameraOptions = null;
    let cameraType = 'INTAKE';
    let scanModeValue = null;
    const scanSessionId = (window.crypto && window.crypto.randomUUID)
        ? window.crypto.randomUUID()
        : `session-${Date.now()}-${Math.random().toString(16).slice(2)}`;

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
        API_CLOSE_SESSION = config.endpoints.closeSession;
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
    const stateBadge = document.getElementById('scan-state-badge');
    const frameBuffer = document.getElementById('frame-buffer');
    const frameCtx = frameBuffer ? frameBuffer.getContext('2d', { willReadFrequently: true }) : null;
    const dataOverlay = document.querySelector('.scan-data-overlay');
    const dataFeed = dataOverlay ? dataOverlay.querySelector('.scan-data-feed') : null;
    const scanFrame = document.querySelector('.scan-frame');
    const duplicateIntakeBtn = document.querySelector('[data-duplicate-intake]');
    const intakeConfirmModal = document.querySelector('[data-intake-confirm-modal]');
    const intakeConfirmBackdrop = document.getElementById('intakeConfirmBackdrop');
    const intakeConfirmSummary = document.querySelector('[data-intake-confirm-summary]');
    const intakeConfirmStatus = document.querySelector('[data-intake-confirm-status]');
    const intakeConfirmDecline = document.querySelector('[data-intake-confirm-decline]');
    const intakeConfirmSubmit = document.querySelector('[data-intake-confirm-submit]');
    const intakeConfirmEndpoint = endpoints && endpoints.intakeConfirm ? endpoints.intakeConfirm : null;

    if (!video) {
        return;
    }

    let currentStream = null;
    let isScreenShare = false;
    const LOOP_INTERVAL_MS = 900;
    const MIN_UPLOAD_GAP_MS = 850;
    const NOTIFICATION_TTL_MS = 6200;
    const NOTIFICATION_FADE_MS = 900;
    const NOTIFICATION_DEDUPE_MS = 1200;
    let loopPaused = false;
    let lastUploadAt = 0;
    let lastNoticeKey = null;
    let lastNoticeAt = 0;
    let pendingIntakePayload = null;
    let duplicateIntakePayload = null;

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
            if (loopPaused) {
                setTimeout(loop, LOOP_INTERVAL_MS);
                return;
            }

            const now = Date.now();
            if (now - lastUploadAt < MIN_UPLOAD_GAP_MS) {
                setTimeout(loop, LOOP_INTERVAL_MS);
                return;
            }

            window.getCurrentFrameBlob(blob => {
                if (!blob) {
                    pushNotification(["Frame capture failed"], "error");
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
                if (scanSessionId) {
                    formData.append('scanSessionId', scanSessionId);
                }

                // USING THE SERVER-PROVIDED ENDPOINT
                const frameHeaders = applyCsrf({});
                const frameOptions = { method: 'POST', body: formData };
                if (Object.keys(frameHeaders).length) {
                    frameOptions.headers = frameHeaders;
                }
                fetch(API_STREAM_FRAME, frameOptions)
                    .then(r => {
                        if (!r.ok) {
                            throw new Error("Frame upload failed");
                        }
                        return r.json();
                    })
                    .then(payload => {
                        updateStateBadge(payload);
                        handleSessionActions(payload);
                        renderDuplicateAction(payload);
                        handleIntakeConfirm(payload);
                        pushPayloadNotification(payload);
                        setTimeout(loop, LOOP_INTERVAL_MS);
                    })
                    .catch(e => {
                        console.error(e);
                        pushNotification(["Frame upload failed"], "error");
                        setTimeout(loop, 5000);
                    });
            });
        };
        loop();
    }

    function handleError(msg, err) {
        console.error(err);
        pushNotification([msg], "error");
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
    if (stateBadge) {
        stateBadge.textContent = 'SEARCHING';
    }
    if (scanFrame) {
        scanFrame.classList.add("is-searching");
    }
    startCamera();

    document.querySelectorAll('[data-scan-close]').forEach((btn) => {
        btn.addEventListener('click', () => closeScanSession());
    });

    function closeScanSession() {
        if (!API_CLOSE_SESSION || !scanSessionId) {
            return;
        }
        const payload = new URLSearchParams();
        payload.set("scanSessionId", scanSessionId);
        if (csrf.token && csrf.param) {
            payload.set(csrf.param, csrf.token);
        }
        if (navigator.sendBeacon) {
            navigator.sendBeacon(API_CLOSE_SESSION, payload);
            return;
        }
        fetch(API_CLOSE_SESSION, {
            method: "POST",
            headers: applyCsrf({ "Content-Type": "application/x-www-form-urlencoded" }),
            body: payload.toString(),
            keepalive: true
        }).catch(() => {});
    }

    window.addEventListener("pagehide", closeScanSession);
    window.addEventListener("beforeunload", closeScanSession);

    function updateStateBadge(payload) {
        if (!stateBadge) {
            return;
        }
        if (payload && (payload.intakeStatus === "already_scanned" || payload.intakeStatus === "processing")) {
            return;
        }
        const rawState = payload && payload.cameraState ? String(payload.cameraState) : "SEARCHING";
        stateBadge.textContent = rawState.toUpperCase();
        if (payload && payload.notes) {
            stateBadge.title = payload.notes;
        } else {
            stateBadge.removeAttribute("title");
        }

        if (!scanFrame) {
            return;
        }
        const next = rawState.toUpperCase();
        scanFrame.classList.remove("is-searching", "is-analyzing", "is-found", "is-scanned", "is-error");
        if (next === "SEARCHING") {
            scanFrame.classList.add("is-searching");
        } else if (next === "ANALYZING") {
            scanFrame.classList.add("is-analyzing");
        } else if (next === "FOUND") {
            scanFrame.classList.add("is-found");
        } else if (next === "SCANNED") {
            scanFrame.classList.add("is-scanned");
        } else if (next === "ERROR") {
            scanFrame.classList.add("is-error");
        }
    }

    function handleSessionActions(payload) {
        if (!payload || payload.cameraState !== "FOUND") {
            return;
        }
        if (cameraType === "QR_SCAN" && payload.uuid) {
            const isEmbedded = window.parent && window.parent !== window;
            if (isEmbedded) {
                window.parent.postMessage({ type: "driverScanFound", uuid: payload.uuid }, window.location.origin);
            } else {
                window.location.href = `/dashboard/driver/state?uuid=${encodeURIComponent(payload.uuid)}`;
            }
            return;
        }
        if (cameraType === "ROUTE") {
            const isEmbedded = window.parent && window.parent !== window;
            if (isEmbedded) {
                window.parent.postMessage({ type: "routeScanResult", routeId, payload }, window.location.origin);
            }
        }
    }

    function renderDuplicateAction(payload) {
        if (!duplicateIntakeBtn) {
            return;
        }
        if (cameraType !== "INTAKE") {
            duplicateIntakeBtn.hidden = true;
            duplicateIntakePayload = null;
            return;
        }
        const isDuplicate = payload
            && payload.intakeStatus === "already_scanned"
            && payload.address
            && !payload.uuid
            && !payload.trackingNumber;
        if (isDuplicate) {
            duplicateIntakePayload = payload;
            duplicateIntakeBtn.hidden = false;
        } else {
            duplicateIntakeBtn.hidden = true;
            duplicateIntakePayload = null;
        }
    }

    function pushPayloadNotification(payload) {
        if (!payload) {
            return;
        }
        const isVerifying = payload.intakeStatus === "address_verifying";
        const isProcessing = payload.intakeStatus === "processing";
        const isError = payload.cameraState === "ERROR";
        const isAlreadyScanned = payload.intakeStatus === "already_scanned";
        const isNotable = isVerifying || isProcessing || isError || payload.cameraState === "FOUND" || payload.cameraState === "SCANNED";
        if (!isNotable) {
            return;
        }

        const lines = [];
        let tone = "neutral";
        if (isVerifying) {
            tone = "verifying";
            lines.push(payload.notes || "Verifying address with LiveAPI...");
        } else if (isProcessing) {
            tone = "neutral";
            lines.push(payload.notes || "Processing scan in background...");
        } else if (isError) {
            tone = "error";
            lines.push(payload.notes || "Scan error");
        } else {
            if (isAlreadyScanned) {
                lines.push("ALREADY SCANNED");
                lines.push("Already scanned in this session.");
            } else if (payload.cameraState === "SCANNED") {
                lines.push(payload.notes || "Already found");
            }
            if (payload.existingLabel) {
                lines.push("Already in system");
            }
            if (payload.name) {
                lines.push(payload.name);
            }
            const addressParts = [payload.address, payload.city, payload.state, payload.zipCode]
                .filter(Boolean)
                .map(part => String(part).trim())
                .filter(part => part.length);
            if (addressParts.length) {
                lines.push(addressParts.join(", "));
            }
            if (payload.trackingNumber) {
                lines.push(`Tracking: ${payload.trackingNumber}`);
            }
            if (!lines.length && payload.notes) {
                lines.push(payload.notes);
            }
        }

        if (!lines.length) {
            return;
        }
        const key = buildNoticeKey(payload, tone);
        pushNotification(lines, tone, key);
    }

    function buildNoticeKey(payload, tone) {
        const parts = [
            tone,
            payload.cameraState,
            payload.intakeStatus,
            payload.uuid,
            payload.trackingNumber,
            payload.address,
            payload.notes
        ].filter(Boolean);
        return parts.join("|");
    }

    function pushNotification(lines, tone, key) {
        if (!dataFeed || !lines || !lines.length) {
            return;
        }
        const now = Date.now();
        if (key && key === lastNoticeKey && now - lastNoticeAt < NOTIFICATION_DEDUPE_MS) {
            return;
        }
        lastNoticeKey = key;
        lastNoticeAt = now;

        const item = document.createElement("div");
        item.className = "scan-data-item";
        if (tone === "error") {
            item.classList.add("is-error");
        } else if (tone === "verifying") {
            item.classList.add("is-verifying");
        }
        lines.forEach((line) => {
            const div = document.createElement("div");
            div.className = "scan-data-line";
            div.textContent = line;
            item.appendChild(div);
        });
        dataFeed.prepend(item);
        requestAnimationFrame(() => item.classList.add("show"));

        setTimeout(() => {
            item.classList.add("fade");
        }, NOTIFICATION_TTL_MS);

        setTimeout(() => {
            item.remove();
        }, NOTIFICATION_TTL_MS + NOTIFICATION_FADE_MS);
    }

    function submitIntake(payload) {
        if (!payload || !intakeConfirmEndpoint) {
            return Promise.reject();
        }
        const body = new URLSearchParams();
        if (payload.uuid) body.set("uuid", payload.uuid);
        if (payload.trackingNumber) body.set("trackingNumber", payload.trackingNumber);
        if (payload.name) body.set("name", payload.name);
        if (payload.address) body.set("address", payload.address);
        if (payload.city) body.set("city", payload.city);
        if (payload.state) body.set("state", payload.state);
        if (payload.zipCode) body.set("zip", payload.zipCode);
        if (payload.country) body.set("country", payload.country);
        if (payload.phoneNumber) body.set("phone", payload.phoneNumber);
        if (payload.deadline) body.set("deadline", payload.deadline);

        return fetch(intakeConfirmEndpoint, {
            method: "POST",
            headers: applyCsrf({ "Content-Type": "application/x-www-form-urlencoded" }),
            body: body.toString()
        }).then((res) => res.ok ? res.json() : Promise.reject());
    }

    function handleIntakeConfirm(payload) {
        if (!intakeConfirmModal || !payload || cameraType !== "INTAKE") {
            return;
        }
        if (payload.cameraState !== "FOUND") {
            return;
        }
        if (payload.intakeStatus === "duplicate_address") {
            return;
        }
        if (!(payload.pendingIntake || payload.existingLabel)) {
            return;
        }
        if (!intakeConfirmModal.hasAttribute("hidden")) {
            return;
        }
        pendingIntakePayload = payload;
        const summary = payload.name && payload.address
            ? `${payload.name} - ${payload.address}${payload.city ? ', ' + payload.city : ''}`
            : (payload.trackingNumber ? `Tracking ${payload.trackingNumber}` : "Waiting for data...");
        if (intakeConfirmSummary) {
            intakeConfirmSummary.textContent = summary;
        }
        if (intakeConfirmStatus) {
            intakeConfirmStatus.textContent = payload.existingLabel ? "Label already exists in the system." : "";
            intakeConfirmStatus.hidden = !payload.existingLabel;
        }
        if (intakeConfirmDecline) {
            intakeConfirmDecline.textContent = payload.existingLabel ? "Close" : "Decline";
            intakeConfirmDecline.hidden = false;
        }
        if (intakeConfirmSubmit) {
            intakeConfirmSubmit.textContent = payload.existingLabel ? "Close" : "Confirm intake";
            intakeConfirmSubmit.hidden = !!payload.existingLabel;
        }

        intakeConfirmModal.removeAttribute("hidden");
        document.body.classList.add("scan-modal-open");
        loopPaused = true;
        requestAnimationFrame(() => intakeConfirmModal.classList.add("open"));
    }

    function closeIntakeConfirm() {
        if (!intakeConfirmModal) return;
        intakeConfirmModal.classList.remove("open");
        document.body.classList.remove("scan-modal-open");
        loopPaused = false;
        setTimeout(() => {
            intakeConfirmModal.setAttribute("hidden", "hidden");
        }, 220);
        pendingIntakePayload = null;
    }

    if (intakeConfirmDecline) {
        intakeConfirmDecline.addEventListener("click", closeIntakeConfirm);
    }

    if (intakeConfirmBackdrop) {
        intakeConfirmBackdrop.addEventListener("click", closeIntakeConfirm);
    }

    if (intakeConfirmSubmit) {
        intakeConfirmSubmit.addEventListener("click", () => {
            if (!pendingIntakePayload || !intakeConfirmEndpoint) {
                closeIntakeConfirm();
                return;
            }
            if (pendingIntakePayload.existingLabel) {
                closeIntakeConfirm();
                return;
            }
            if (intakeConfirmStatus) {
                intakeConfirmStatus.textContent = "Processing...";
                intakeConfirmStatus.hidden = false;
            }

            const payload = pendingIntakePayload;
            submitIntake(payload)
                .then(() => {
                    pushNotification(["Intake confirmed"], "neutral");
                })
                .catch(() => {
                    if (intakeConfirmStatus) {
                        intakeConfirmStatus.textContent = "Unable to confirm intake.";
                        intakeConfirmStatus.hidden = false;
                    }
                })
                .finally(closeIntakeConfirm);
        });
    }

    if (duplicateIntakeBtn) {
        duplicateIntakeBtn.addEventListener("click", () => {
            if (!duplicateIntakePayload || !intakeConfirmEndpoint) {
                return;
            }
            duplicateIntakeBtn.disabled = true;
            const originalText = duplicateIntakeBtn.textContent;
            duplicateIntakeBtn.textContent = "Creating...";
            submitIntake(duplicateIntakePayload)
                .then(() => {
                    pushNotification(["Additional label created"], "neutral");
                })
                .catch(() => {
                    pushNotification(["Unable to create label"], "error");
                })
                .finally(() => {
                    duplicateIntakeBtn.disabled = false;
                    duplicateIntakeBtn.textContent = originalText;
                });
        });
    }

})();
