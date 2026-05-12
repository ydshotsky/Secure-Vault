let revealedStatus = {};

async function revealPassword(id) {
    if (revealedStatus[id]) {
        document.getElementsByClassName("psw" + id)[0].innerHTML = "********";
        revealedStatus[id] = false;
        return;
    }
    const url = `/password/reveal/${id}`;
    const csrftoken = document.querySelector('meta[name="_csrf"]').content;
    const csrfHeader = document.querySelector('meta[name="_csrf_header"]').content;
    try {
        const res = await fetch(url, {
            method: "POST",
            headers: {
                [csrfHeader]: csrftoken
            }
        });

        if (res.ok) {
            const data = await res.text();
            document.getElementsByClassName("psw" + id)[0].innerHTML = data;
            revealedStatus[id] = true;
        } else if (res.status === 423) {
            showUnlockModal();
        }
    } catch (error) {
        console.error("Failed to fetch password:", error);
    }

}

async function deletePassword(id) {
    const url = `/password/delete/${id}`;
    const res=confirm("Are you sure you want to delete this password?");
    if (!res)return;
    const csrftoken = document.querySelector('meta[name="_csrf"]').content;
    const csrfHeader = document.querySelector('meta[name="_csrf_header"]').content;

    try {
        const res = await fetch(url, {
            method: "DELETE",
            headers: {
                [csrfHeader]: csrftoken
            }
        });
        if (res.ok) {
            alert("password deleted successfully.");
            window.location.reload();
        } else if (res.status === 423) {
            showUnlockModal();
        }
    } catch (error) {
        console.error("Failed to delete password:", error);
    }


}