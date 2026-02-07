(function () {
    const toast = () => document.getElementById("toast");
    const text = () => document.getElementById("toastText");
    const meta = () => document.getElementById("toastMeta");
    const bar = () => document.getElementById("toastBar");

    let hideTimer = null;
    let progressTimer = null;

    function show(message, metaText, durationMs) {
        const t = toast();
        const tx = text();
        const mt = meta();
        const br = bar();

        if (!t || !tx || !mt || !br) return;

        tx.textContent = message || "";
        mt.textContent = metaText || "";
        t.classList.add("show");

        const ms = typeof durationMs === "number" ? durationMs : 2500;
        const end = Date.now() + ms;

        br.style.width = "100%";
        clearInterval(progressTimer);

        progressTimer = setInterval(function () {
            const remaining = Math.max(0, end - Date.now());
            br.style.width = ms > 0 ? Math.round((remaining / ms) * 100) + "%" : "0%";
            if (remaining <= 0) {
                clearInterval(progressTimer);
                br.style.width = "0%";
            }
        }, 80);

        clearTimeout(hideTimer);
        hideTimer = setTimeout(function () {
            t.classList.remove("show");
        }, ms + 150);
    }

    window.tavallToast = function (message, metaText, durationMs) {
        show(message, metaText, durationMs);
    };
})();