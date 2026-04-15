const cnpjInput = document.getElementById('cnpj');
const btnConsultar = document.getElementById('btnConsultarCnpj');
const form = document.getElementById('formCadastroEmpresa');
const loading = document.getElementById('loadingOverlay');
const alertContainer = document.getElementById('alertContainer');

let debounceTimer;

function showLoading() {
    loading.style.display = 'flex';
}

function hideLoading() {
    loading.style.display = 'none';
}

function showAlert(message, type = 'error') {
    alertContainer.innerHTML = `
        <div class=\"alert alert-${type}\">
            ${message}
        </div>
    `;

    setTimeout(() => {
        alertContainer.innerHTML = '';
    }, 5000);
}

function onlyNumbers(value) {
    return value.replace(/\D/g, '');
}

function setValid(el) {
    el.style.borderColor = '#10b981';
}

function setInvalid(el) {
    el.style.borderColor = '#ef4444';
}

function resetBorder(el) {
    el.style.borderColor = '';
}

// ==================== CNPJ MASK ====================
cnpjInput.addEventListener('input', (e) => {
    let v = onlyNumbers(e.target.value);

    if (v.length > 14) v = v.slice(0, 14);

    // Format: 00.000.000/0000-00
    v = v.replace(/^(\d{2})(\d)/, '$1.$2');
    v = v.replace(/^(\d{2})\.(\d{3})(\d)/, '$1.$2.$3');
    v = v.replace(/\.(\d{3})(\d)/, '.$1/$2');
    v = v.replace(/(\d{4})(\d)/, '$1-$2');

    e.target.value = v;

    if (onlyNumbers(v).length === 14) {
        clearTimeout(debounceTimer);
        debounceTimer = setTimeout(() => consultarCnpj(), 600);
    }
});

async function consultarCnpj(retry = 1) {
    const cnpj = onlyNumbers(cnpjInput.value);

    if (cnpj.length !== 14) {
        setInvalid(cnpjInput);
        showAlert('CNPJ inválido. Digite um CNPJ com 14 dígitos.', 'error');
        return;
    }

    showLoading();
    btnConsultar.disabled = true;
    resetBorder(cnpjInput);

    try {
        const response = await fetch(`/api/v1/empresas/consultar-cnpj/${cnpj}`, {
            method: 'GET',
            headers: {
                'Accept': 'application/json'
            },
            credentials: 'include'
        });

        if (!response.ok) {
            if (response.status === 404) {
                throw new Error('CNPJ não encontrado na base da Receita Federal');
            }
            throw new Error('Erro ao consultar CNPJ');
        }

        const data = await response.json();

        preencherFormulario(data);
        revelarFormulario();

        setValid(cnpjInput);
        showAlert('Dados da empresa encontrados com sucesso!', 'success');

    } catch (error) {
        console.error('Erro ao consultar CNPJ:', error);

        if (retry > 0) {
            console.log('Tentando novamente...');
            return consultarCnpj(retry - 1);
        }

        setInvalid(cnpjInput);
        showAlert(error.message || 'Erro ao buscar CNPJ. Tente novamente.', 'error');

    } finally {
        hideLoading();
        btnConsultar.disabled = false;
    }
}

btnConsultar.addEventListener('click', () => consultarCnpj());

function revelarFormulario() {
    form.classList.remove('hidden');

    // Smooth animation
    form.style.opacity = '0';
    form.style.transform = 'translateY(10px)';

    requestAnimationFrame(() => {
        form.style.transition = 'all 0.4s ease';
        form.style.opacity = '1';
        form.style.transform = 'translateY(0)';
    });
}

function preencherFormulario(data) {
    setField('razaoSocial', data.razao_social || data.nome);
    setField('nomeFantasia', data.nome_fantasia || data.fantasia);
    setField('email', data.email);
    setField('telefone', data.telefone || data.ddd_telefone_1);

    if (data.logradouro) setField('logradouro', data.logradouro);
    if (data.numero) setField('numero', data.numero);
    if (data.bairro) setField('bairro', data.bairro);
    if (data.municipio) setField('municipio', data.municipio);
    if (data.uf) setField('uf', data.uf);
    if (data.cep) setField('cep', data.cep);
}

function setField(id, value) {
    const el = document.getElementById(id);
    if (!el) return;

    el.value = value || '';
    if (value) setValid(el);
}

form.querySelectorAll('input').forEach(input => {
    input.addEventListener('blur', () => {
        if (input.required && !input.value.trim()) {
            setInvalid(input);
        } else if (input.value.trim()) {
            setValid(input);
        } else {
            resetBorder(input);
        }
    });

    input.addEventListener('focus', () => {
        resetBorder(input);
    });
});

// ==================== PHONE MASK ====================
const telefoneInput = document.getElementById('telefone');
if (telefoneInput) {
    telefoneInput.addEventListener('input', (e) => {
        let value = onlyNumbers(e.target.value);

        if (value.length <= 10) {
            // (00) 0000-0000
            value = value.replace(/^(\d{2})(\d)/, '($1) $2');
            value = value.replace(/(\d{4})(\d)/, '$1-$2');
        } else {
            // (00) 00000-0000
            value = value.slice(0, 11);
            value = value.replace(/^(\d{2})(\d)/, '($1) $2');
            value = value.replace(/(\d{5})(\d)/, '$1-$2');
        }

        e.target.value = value;
    });
}

// ==================== FORM SUBMISSION ====================
form.addEventListener('submit', async (e) => {
    e.preventDefault();

    // Build payload
    const formData = new FormData(form);
    const payload = {
        cnpj: onlyNumbers(formData.get('cnpj') || cnpjInput.value),
        razaoSocial: formData.get('razaoSocial'),
        nomeFantasia: formData.get('nomeFantasia'),
        email: formData.get('email'),
        telefone: onlyNumbers(formData.get('telefone')),
        inscricaoEstadual: formData.get('inscricaoEstadual'),
        inscricaoMunicipal: formData.get('inscricaoMunicipal'),
        site: formData.get('site')
    };

    // Validate required fields
    if (!payload.cnpj || payload.cnpj.length !== 14) {
        showAlert('CNPJ é obrigatório e deve ter 14 dígitos.', 'error');
        return;
    }

    showLoading();

    try {
        const response = await fetch('/api/v1/empresas', {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json',
                'Accept': 'application/json'
            },
            credentials: 'include',
            body: JSON.stringify(payload)
        });

        if (!response.ok) {
            const errorData = await response.json().catch(() => ({}));
            throw new Error(errorData.mensagem || 'Erro ao cadastrar empresa');
        }

        const data = await response.json();

        // Success - redirect to planos page
        showAlert('Empresa cadastrada com sucesso!', 'success');

        setTimeout(() => {
            window.location.href = '/planos';
        }, 1000);

    } catch (error) {
        console.error('Erro ao cadastrar empresa:', error);
        showAlert(error.message || 'Erro ao cadastrar empresa. Tente novamente.', 'error');
    } finally {
        hideLoading();
    }
});

// ==================== PAGE LOAD ====================
document.addEventListener('DOMContentLoaded', () => {
    console.log('Página de cadastro de empresa carregada');
    cnpjInput.focus();
});