const cnpjInput = document.getElementById('cnpj');
const btnConsultar = document.getElementById('btnConsultarCnpj');
const form = document.getElementById('formCadastroEmpresa');
const loading = document.getElementById('loadingOverlay');
const alertContainer = document.getElementById('alertContainer');

let debounceTimer;
let consultaEmAndamento = false;

function showLoading() {
    loading.style.display = 'flex';
}

function hideLoading() {
    loading.style.display = 'none';
}

function showAlert(message, type = 'error') {
    alertContainer.innerHTML = `<div class="alert alert-${type}">${message}</div>`;
    setTimeout(() => {
        alertContainer.innerHTML = '';
    }, 5000);
}

function onlyNumbers(value) {
    if (!value) return '';
    return value.replace(/\D/g, '');
}

function setValid(el) {
    if (el) el.style.borderColor = '#10b981';
}

function setInvalid(el) {
    if (el) el.style.borderColor = '#ef4444';
}

function resetBorder(el) {
    if (el) el.style.borderColor = '';
}

function validarCnpj(cnpj) {
    cnpj = onlyNumbers(cnpj);
    if (cnpj.length !== 14) return false;
    if (/^(\d)\1{13}$/.test(cnpj)) return false;
    let tamanho = cnpj.length - 2;
    let numeros = cnpj.substring(0, tamanho);
    let digitos = cnpj.substring(tamanho);
    let soma = 0;
    let pos = tamanho - 7;
    for (let i = tamanho; i >= 1; i--) {
        soma += Number(numeros.charAt(tamanho - i)) * pos--;
        if (pos < 2) pos = 9;
    }
    let resultado = soma % 11 < 2 ? 0 : 11 - (soma % 11);
    if (resultado !== parseInt(digitos.charAt(0), 10)) return false;
    tamanho = tamanho + 1;
    numeros = cnpj.substring(0, tamanho);
    soma = 0;
    pos = tamanho - 7;
    for (let i = tamanho; i >= 1; i--) {
        soma += Number(numeros.charAt(tamanho - i)) * pos--;
        if (pos < 2) pos = 9;
    }
    resultado = soma % 11 < 2 ? 0 : 11 - (soma % 11);
    return resultado === parseInt(digitos.charAt(1), 10);
}

cnpjInput.addEventListener('input', (e) => {
    let v = onlyNumbers(e.target.value);
    if (v.length > 14) v = v.slice(0, 14);
    v = v.replace(/^(\d{2})(\d)/, '$1.$2');
    v = v.replace(/^(\d{2})\.(\d{3})(\d)/, '$1.$2.$3');
    v = v.replace(/\.(\d{3})(\d)/, '.$1/$2');
    v = v.replace(/(\d{4})(\d)/, '$1-$2');
    e.target.value = v;
    const cnpjLimpo = onlyNumbers(v);
    if (cnpjLimpo.length === 14) {
        clearTimeout(debounceTimer);
        debounceTimer = setTimeout(() => {
            if (validarCnpj(cnpjLimpo)) {
                consultarCnpj();
            } else {
                setInvalid(cnpjInput);
                showAlert('⚠️ CNPJ inválido.', 'error');
            }
        }, 800);
    } else {
        resetBorder(cnpjInput);
    }
});

async function consultarCnpj(retry = 1) {
    const cnpj = onlyNumbers(cnpjInput.value);
    if (cnpj.length !== 14 || !validarCnpj(cnpj)) {
        setInvalid(cnpjInput);
        showAlert('❌ CNPJ inválido.', 'error');
        return;
    }
    if (consultaEmAndamento) return;
    clearTimeout(debounceTimer);
    consultaEmAndamento = true;
    showLoading();
    btnConsultar.disabled = true;
    btnConsultar.textContent = 'Consultando...';
    resetBorder(cnpjInput);
    try {
        const response = await fetch(`/api/v1/empresas/consultar-cnpj/${cnpj}`, {
            method: 'GET',
            headers: { 'Accept': 'application/json' }
        });
        if (!response.ok) {
            if (response.status === 404) {
                showAlert('⚠️ CNPJ não encontrado. Preencha manualmente.', 'warning');
                revelarFormulario();
                setValid(cnpjInput);
                return;
            }
            throw new Error(`Erro ao consultar CNPJ.`);
        }
        const data = await response.json();
        preencherFormulario(data);
        revelarFormulario();
        setValid(cnpjInput);
        showAlert('✅ Dados encontrados!', 'success');
    } catch (error) {
        if (retry > 0 && error.message.includes('Failed to fetch')) {
            consultaEmAndamento = false;
            return consultarCnpj(retry - 1);
        }
        setInvalid(cnpjInput);
        showAlert('❌ ' + error.message, 'error');
    } finally {
        hideLoading();
        btnConsultar.disabled = false;
        btnConsultar.textContent = 'Consultar';
        consultaEmAndamento = false;
    }
}

btnConsultar.addEventListener('click', () => consultarCnpj());

function revelarFormulario() {
    form.classList.remove('hidden');
}

function preencherFormulario(data) {
    setField('razaoSocial', data.razaoSocial || data.razao_social || data.nome);
    setField('nomeFantasia', data.nomeFantasia || data.nome_fantasia || data.fantasia);
    setField('email', data.email);
    setField('telefone', data.telefone || data.ddd_telefone_1);
    const select = document.getElementById("segmentos");
    if (data.segmentos && select) select.value = data.segmentos;
    setField('inscricaoEstadual', data.inscricaoEstadual || data.inscricao_estadual);
    setField('inscricaoMunicipal', data.inscricaoMunicipal || data.inscricao_municipal);
    setField('site', data.site);
}

async function carregarSegmentos() {
    const select = document.getElementById("segmentos");
    if (!select) return;
    try {
        const response = await fetch("/api/v1/empresas/segmentos");
        if (!response.ok) throw new Error();
        const data = await response.json();
        select.innerHTML = '<option value="" disabled selected>Selecione um segmento</option>';
        data.forEach(seg => {
            const option = document.createElement("option");
            option.value = seg.value || seg.name || seg;
            option.textContent = seg.label || seg.nome || seg;
            select.appendChild(option);
        });
    } catch (error) {
        showAlert("Erro ao carregar segmentos", "error");
    }
}

function setField(id, value) {
    const el = document.getElementById(id);
    if (el) {
        el.value = value || '';
        if (value) setValid(el);
    }
}

form.addEventListener('submit', async (e) => {
    e.preventDefault();
    const formData = new FormData(form);
    const payload = {
        cnpj: onlyNumbers(cnpjInput.value),
        razaoSocial: formData.get('razaoSocial'),
        nomeFantasia: formData.get('nomeFantasia'),
        email: formData.get('email'),
        telefone: onlyNumbers(formData.get('telefone')),
        segmentos: formData.get('segmentos'),
        inscricaoEstadual: formData.get('inscricaoEstadual'),
        inscricaoMunicipal: formData.get('inscricaoMunicipal'),
        site: formData.get('site')
    };
    const token = localStorage.getItem('accessToken');
    if (!token) {
        window.location.href = '/login';
        return;
    }
    showLoading();
    try {
        const response = await fetch('/api/v1/empresas', {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json',
                'Authorization': `Bearer ${token}`
            },
            body: JSON.stringify(payload)
        });
        if (!response.ok) throw new Error('Erro ao cadastrar empresa');
        showAlert('✅ Empresa cadastrada!', 'success');
        setTimeout(() => { window.location.href = '/planos'; }, 1200);
    } catch (error) {
        showAlert('❌ ' + error.message, 'error');
    } finally {
        hideLoading();
    }
});

document.addEventListener("DOMContentLoaded", () => {
    carregarSegmentos();
    cnpjInput.focus();
});