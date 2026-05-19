let revealedStatus = {};
const csrftoken = document.querySelector('meta[name="_csrf"]').content;
const csrfHeader = document.querySelector('meta[name="_csrf_header"]').content;

function isVaultLocked(){
   const url='/vault/is-unlocked';

   const res=fetch(url,{
       method: 'GET',
   headers:{
           [csrfHeader]:csrfToken
   }});
   return res.data === true;
}

async function revealPassword(id) {
    if (revealedStatus[id]) {
        document.getElementsByClassName("psw" + id)[0].innerHTML = "********";
        revealedStatus[id] = false;
        return;
    }
    const url = `/password/reveal/${id}`;
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
    const res = confirm("Are you sure you want to delete this password?");
    if (!res) return;
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

async function editPassword(id) {
    const row = document.getElementById("row" + id);
    if (!row) {
        alert("Row not found for edit.");
        return;
    }
    const password = prompt("New password:", "");
    if (password === null || password.trim() === "") {
        alert("Password is required.");
        return;
    }

    const url = `/password/edit/${id}`;
    try {
        const res = await fetch(url, {
            method: "PUT",
            headers: {
                [csrfHeader]: csrftoken,
                "Content-Type": "application/json"
            },
            body: JSON.stringify({
                password: password
            })
        });

        if (res.ok) {
            alert("password updated successfully.");
        } else if (res.status === 423) {
            showUnlockModal();
            alert("you can now update your password.");
        } else {
            const contentType = res.headers.get("content-type");
            const errorText = await res.text();
            if (contentType && contentType.includes("text/html")) {
                document.open();
                document.write(errorText);
                document.close();
            } else {
                alert(errorText || "Failed to update password.");
            }
        }
    } catch (error) {
        console.error("Failed to update password:", error);
    }
}
