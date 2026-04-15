const cnpjInput = document.getElementById("cnpj");
const btnConsultar = document.getElementById("btnConsultarCnpj");
const form = document.getElementById("formCadastroEmpresa");
const loading = document.getElementById("loadingOverlay");
const alertContainer = document.getElementById("alertContainer");

let debounceTimer;

/* =========================
   LOADING
========================= */
function showLoading() {
    loading.style.display = "flex";
}

function hideLoading() {
    loading.style.display = "none";
}

/* =========================
   ALERT SYSTEM
========================= */
function showAlert(message, type = "error") {
    alertContainer.innerHTML = `
        <div class="alert alert-${type}">
            ${message}
        </div>
    `;
}

/* =========================
   HELPERS
========================= */
function onlyNumbers(value) {
    return value.replace(/\D/g, "");
}

/* =========================
   VISUAL VALIDATION
========================= */
function setValid(el) {
    el.style.borderColor = "#22c55e";
}

function setInvalid(el) {
    el.style.borderColor = "#ef4444";
}

/* =========================
   MASK CNPJ
========================= */
cnpjInput.addEventListener("input", (e) => {
    let v = onlyNumbers(e.target.value);

    if (v.length > 14) v = v.slice(0, 14);

    v = v
        .replace(/^(\d{2})(\d)/, "$1.$2")
        .replace(/^(\d{2})\.(\d{3})(\d)/, "$1.$2.$3")
        .replace(/\.(\d{3})(\d)/, ".$1/$2")
        .replace(/(\d{4})(\d)/, "$1-$2");

    e.target.value = v;

    if (v.replace(/\D/g, "").length === 14) {
        clearTimeout(debounceTimer);
        debounceTimer = setTimeout(() => consultarCnpj(), 600);
    }
});

/* =========================
   CONSULT CNPJ (ROBUST)
========================= */
async function consultarCnpj(retry = 1) {
    const cnpj = onlyNumbers(cnpjInput.value);

    if (cnpj.length !== 14) {
        setInvalid(cnpjInput);
        showAlert("CNPJ inválido");
        return;
    }

    showLoading();
    btnConsultar.disabled = true;

    try {
        const response = await fetch(`/api/cnpj/${cnpj}`, {
            method: "GET",
            headers: {
                "Accept": "application/json"
            }
        });

        if (!response.ok) {
            throw new Error("API_ERROR");
        }

        const data = await response.json();

        preencherFormulario(data);
        revelarFormulario();

        setValid(cnpjInput);
        showAlert("Empresa encontrada com sucesso", "success");

    } catch (error) {

        if (retry > 0) {
            return consultarCnpj(retry - 1);
        }

        setInvalid(cnpjInput);
        showAlert("Erro ao buscar CNPJ. Tente novamente.");

    } finally {
        hideLoading();
        btnConsultar.disabled = false;
    }
}

btnConsultar.addEventListener("click", consultarCnpj);

/* =========================
   SHOW FORM SMOOTH
========================= */
function revelarFormulario() {
    form.classList.remove("hidden");

    form.style.opacity = "0";
    form.style.transform = "translateY(10px)";

    requestAnimationFrame(() => {
        form.style.transition = "0.35s ease";
        form.style.opacity = "1";
        form.style.transform = "translateY(0)";
    });
}

/* =========================
   AUTO FILL FORM
========================= */
function preencherFormulario(data) {
    setField("razaoSocial", data.razao_social);
    setField("nomeFantasia", data.nome_fantasia);
    setField("telefone", data.telefone);

    setField("logradouro", data.logradouro);
    setField("numero", data.numero);
    setField("bairro", data.bairro);
    setField("municipio", data.municipio);
    setField("uf", data.uf);
    setField("cep", data.cep);
}

function setField(id, value) {
    const el = document.getElementById(id);
    if (!el) return;

    el.value = value || "";
    if (value) setValid(el);
}

/* =========================
   INPUT VALIDATION LIVE
========================= */
form.querySelectorAll("input").forEach(input => {
    input.addEventListener("blur", () => {
        if (input.required && !input.value.trim()) {
            setInvalid(input);
        } else {
            setValid(input);
        }
    });
});

/* =========================
   SUBMIT FORM
========================= */
form.addEventListener("submit", async (e) => {
    e.preventDefault();

    const payload = Object.fromEntries(new FormData(form).entries());

    showLoading();

    try {
        const response = await fetch("/empresa", {
            method: "POST",
            headers: {
                "Content-Type": "application/json"
            },
            body: JSON.stringify(payload)
        });

        if (!response.ok) {
            throw new Error("SAVE_ERROR");
        }

        window.location.href = "/planos";

    } catch (error) {
        showAlert("Erro ao salvar empresa. Tente novamente.");
    } finally {
        hideLoading();
    }
});