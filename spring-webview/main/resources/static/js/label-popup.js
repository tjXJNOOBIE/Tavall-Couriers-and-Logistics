(function () {
    function getElements() {
        const modal = document.getElementById("labelModal");
        const frame = document.getElementById("labelModalFrame");
        const closeBtn = document.getElementById("labelModalClose");
        const backdrop = document.getElementById("labelModalBackdrop");
        const panel = modal ? modal.querySelector(".label-popup-panel") : null;
        return { modal, frame, closeBtn, backdrop, panel };
    }

    const TRANSITION_MS = 220;

    function openModal(url) {
        const { modal, frame, panel } = getElements();
        if (!modal || !frame) return;
        frame.src = url;
        frame.style.height = "0px";
        if (panel) {
            panel.style.height = "auto";
        }
        modal.removeAttribute("hidden");
        document.body.classList.add("scan-modal-open");
        requestAnimationFrame(() => modal.classList.add("open"));
    }

    function closeModal() {
        const { modal, frame } = getElements();
        if (!modal || !frame) return;
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

    const { closeBtn, backdrop } = getElements();
    if (closeBtn) {
        closeBtn.addEventListener("click", closeModal);
    }
    if (backdrop) {
        backdrop.addEventListener("click", closeModal);
    }

    document.addEventListener("click", (event) => {
        const link = event.target.closest('a[data-label-popup="true"]');
        if (!link) return;
        if (shouldBypass(event)) return;
        const { modal, frame } = getElements();
        if (!modal || !frame) return;
        event.preventDefault();
        openModal(link.href);
    });

    window.addEventListener("message", (event) => {
        if (event.origin !== window.location.origin) return;
        const { frame, panel } = getElements();
        if (frame && event.source !== frame.contentWindow) return;
        if (event.data && event.data.type === "labelModalClose") {
            closeModal();
        }
        if (event.data && event.data.type === "labelModalResize") {
            const height = Number(event.data.height) || 0;
            if (height <= 0) return;
            const maxHeight = Math.floor(window.innerHeight * 0.92);
            const target = Math.min(height, maxHeight);
            if (frame) {
                frame.style.height = `${target}px`;
            }
            if (panel) {
                panel.style.height = `${target}px`;
            }
        }
    });

    document.addEventListener("keydown", (event) => {
        if (event.key !== "Escape") return;
        const { modal } = getElements();
        if (!modal || modal.hasAttribute("hidden")) return;
        closeModal();
    });
})();
