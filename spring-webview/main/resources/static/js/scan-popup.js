(function () {
    const endpoints = window.APP && window.APP.endpoints ? window.APP.endpoints : null;
    if (!endpoints || !endpoints.merchantScanBase || !endpoints.driverScanBase) {
        throw new Error("Missing endpoint(s): merchantScanBase, driverScanBase");
    }

    const selector = `a[data-scan-popup="true"][href^="${endpoints.merchantScanBase}"], a[data-scan-popup="true"][href^="${endpoints.driverScanBase}"]`;
    const popupLinks = document.querySelectorAll(selector);
    if (!popupLinks.length) return;

    const modal = document.getElementById("scanModal");
    const frame = document.getElementById("scanModalFrame");
    const closeBtn = document.getElementById("scanModalClose");
    const backdrop = document.getElementById("scanModalBackdrop");
    const panel = modal ? modal.querySelector(".scan-modal-panel") : null;

    if (!modal || !frame) return;

    const TRANSITION_MS = 220;

    function openModal(url) {
        frame.src = url;
        modal.removeAttribute("hidden");
        document.body.classList.add("scan-modal-open");
        requestAnimationFrame(() => modal.classList.add("open"));
    }

    function closeModal() {
        modal.classList.remove("open");
        document.body.classList.remove("scan-modal-open");
        setTimeout(() => {
            frame.src = "about:blank";
            modal.setAttribute("hidden", "hidden");
        }, TRANSITION_MS);
    }

    function shouldBypass(event) {
        return event.defaultPrevented
            || event.metaKey
            || event.ctrlKey
            || event.shiftKey
            || event.altKey;
    }

    popupLinks.forEach((link) => {
        link.addEventListener("click", (event) => {
            if (shouldBypass(event)) return;
            event.preventDefault();
            openModal(link.href);
        });
    });

    if (closeBtn) {
        closeBtn.addEventListener("click", closeModal);
    }

    if (backdrop) {
        backdrop.addEventListener("click", closeModal);
    }

    document.addEventListener("keydown", (event) => {
        if (event.key === "Escape" && !modal.hasAttribute("hidden")) {
            closeModal();
        }
    });

    window.addEventListener("message", (event) => {
        if (event.origin !== window.location.origin) return;
        if (frame && event.source !== frame.contentWindow) return;
        if (event.data && (event.data.type === "scanModalClose" || event.data.type === "driverScanFound")) {
            closeModal();
        }
        if (event.data && event.data.type === "scanModalResize") {
            const height = Number(event.data.height) || 0;
            if (height <= 0) return;
            const maxHeight = Math.floor(window.innerHeight * 0.92);
            const target = Math.min(height, maxHeight);
            frame.style.height = `${target}px`;
            if (panel) {
                panel.style.height = `${target}px`;
            }
        }
    });
})();