(function () {
    const endpoints = window.APP && window.APP.endpoints ? window.APP.endpoints : null;
    if (!endpoints || !endpoints.register) {
        throw new Error("Missing endpoint: register");
    }
    const REGISTER_ENDPOINT = endpoints.register;

    const registerBtn = document.getElementById("registerBtn");
    const toast = document.getElementById("toast");
    const toastText = document.getElementById("toastText");
    const toastMeta = document.getElementById("toastMeta");
    const toastBar = document.getElementById("toastBar");

    if (!registerBtn || !toast || !toastText || !toastMeta || !toastBar) return;

    const DEFAULT_COOLDOWN_MS = 4000;

    let cooldownTimer = null;
    let cooldownEndsAt = 0;

    function getCsrf() {
        const token = document.querySelector('meta[name="_csrf"]')?.getAttribute("content") || "";
        const header = document.querySelector('meta[name="_csrf_header"]')?.getAttribute("content") || "";
        return { token, header };
    }

    function showToast(message) {
        toastText.textContent = message;
        toast.classList.add("show");
    }

    function hideToast() {
        toast.classList.remove("show");
    }

    function startCooldown(ms) {
        clearInterval(cooldownTimer);

        registerBtn.disabled = true;
        cooldownEndsAt = Date.now() + ms;

        toastBar.style.width = "100%";

        cooldownTimer = setInterval(function () {
            const remaining = Math.max(0, cooldownEndsAt - Date.now());
            const pct = remaining / ms;

            toastMeta.textContent = "cooldown: " + Math.ceil(remaining / 1000) + "s";
            toastBar.style.width = Math.round(pct * 100) + "%";

            if (remaining <= 0) {
                clearInterval(cooldownTimer);
                cooldownTimer = null;

                registerBtn.disabled = false;
                toastMeta.textContent = "ready";
                toastBar.style.width = "0%";

                setTimeout(hideToast, 600);
            }
        }, 100);
    }

    async function postRegister() {
        const csrf = getCsrf();
        const headers = {};

        if (csrf.header && csrf.token) {
            headers[csrf.header] = csrf.token;
        }

        const res = await fetch(REGISTER_ENDPOINT, {
            method: "POST",
            headers,
            credentials: "same-origin"
        });

        let payload = {};
        try { payload = await res.json(); } catch (e) { payload = {}; }

        const retryAfter = parseInt(res.headers.get("Retry-After") || "0", 10);
        const cooldownMs = retryAfter > 0 ? retryAfter * 1000 : DEFAULT_COOLDOWN_MS;

        if (res.status === 429) {
            return { ok: false, message: payload.error || "registration is coming soon", cooldownMs };
        }

        if (!res.ok) {
            return { ok: false, message: "Request failed (" + res.status + ")", cooldownMs };
        }

        return { ok: true, message: payload.message || "registration is coming soon", cooldownMs };
    }

    registerBtn.addEventListener("click", async function () {
        showToast("registration is coming soon");
        toastMeta.textContent = "cooldown: ...";
        toastBar.style.width = "100%";

        try {
            const result = await postRegister();
            showToast(result.message);
            startCooldown(result.cooldownMs);
        } catch (e) {
            showToast("Server unreachable (check proxy/routes)");
            startCooldown(DEFAULT_COOLDOWN_MS);
        }
    });
})();
