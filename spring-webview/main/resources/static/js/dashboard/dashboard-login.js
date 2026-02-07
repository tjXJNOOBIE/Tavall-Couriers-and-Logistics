(function () {
    const form = document.querySelector('form[action="/dashboard/login"], form[action$="/dashboard/login"]');
    const user = document.getElementById("username");
    const pass = document.getElementById("password");

    if (!form || !user || !pass) return;

    const buttons = document.querySelectorAll(".demo-login");
    if (!buttons || buttons.length === 0) return;

    function setValue(el, value) {
        el.value = value;
        el.dispatchEvent(new Event("input", { bubbles: true }));
        el.dispatchEvent(new Event("change", { bubbles: true }));
    }

    buttons.forEach(function (btn) {
        btn.addEventListener("click", function () {
            const username = btn.getAttribute("data-username") || "";
            const password = btn.getAttribute("data-password") || "";

            setValue(user, username);
            setValue(pass, password);

            // submit like a normal login. Spring Security remains the boss.
            form.submit();
        });
    });
})();