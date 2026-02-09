(function () {
    const modal = document.getElementById("addUserModal");
    const openBtn = document.getElementById("openAddUser");
    const openInlineBtn = document.getElementById("openAddUserInline");
    const closeBtn = document.getElementById("closeAddUser");

    if (!modal || !closeBtn) return;

    if (openBtn) {
        openBtn.addEventListener("click", () => modal.showModal());
    }
    if (openInlineBtn) {
        openInlineBtn.addEventListener("click", () => modal.showModal());
    }
    closeBtn.addEventListener("click", () => modal.close());

    modal.addEventListener("click", (event) => {
        const rect = modal.getBoundingClientRect();
        const inside = event.clientX >= rect.left
            && event.clientX <= rect.right
            && event.clientY >= rect.top
            && event.clientY <= rect.bottom;
        if (!inside) {
            modal.close();
        }
    });
})();
