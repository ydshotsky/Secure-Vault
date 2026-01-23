

const csrfToken=document.querySelector('meta[name="_csrf"]').content;
const csrfHeader=document.querySelector('meta[name="_csrf_header"]').content;



async function savePassword() {

    const form = document.getElementById("save-form");

    if (!form.reportValidity()) {
        return; // browser shows validation messages
    }

    const res=await fetch("/password/save-vault-password",{
        method: "POST",
        headers:{
            [csrfHeader]:csrfToken,
        },
        body:new FormData(document.getElementById("save-form"))
    });

    if(res.status === 200){
        alert("Password has been saved!");
    }
    if(res.status === 423){
        alert("vault is locked,please unlock the vault.");
        showUnlockModal();
    }
    else {
        alert("Password has not been saved!");
    }

}

