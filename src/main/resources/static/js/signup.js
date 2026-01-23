const usernameInput = document.getElementById('username');
const statusSpan = document.getElementById('username-status');
usernameInput.addEventListener('blur', async function () {
    const username = usernameInput.value.trim();
    if (username === "") {
        statusSpan.textContent = "";
        return;
    }
    try {
        const res = await fetch(`/users/username-availability?username=${encodeURIComponent(username)}`, {
            method: "GET",
            redirect: "manual"
        });
        if (res.status === 200) {
            const data = await res.text();
            if (data === "true") {
                statusSpan.textContent = "username available";
                statusSpan.style.color = "blue";
                document.getElementById("submit-btn").disabled=false;
                document.getElementById("submit-btn").classList.remove("disabled-style");


            } else {
                statusSpan.textContent = "username not available";
                statusSpan.style.color = "red";

                document.getElementById("submit-btn").disabled = true;
                document.getElementById("submit-btn").classList.add("disabled-style");



            }
        } else {
            statusSpan.textContent = "server not responding,please try again later.";
        }
    } catch (err) {
        statusSpan.textContent = "unable to check username";
        statusSpan.style.color = "orange";
    }
});

