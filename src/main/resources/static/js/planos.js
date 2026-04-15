"// ==================== ELEMENTS ====================
const modal = document.getElementById('checkoutModal');
const checkoutForm = document.getElementById('checkoutForm');
const loadingOverlay = document.getElementById('loadingOverlay');
const alertContainer = document.getElementById('alertContainer');
const creditCardFields = document.getElementById('creditCardFields');
const paymentMethodRadios = document.querySelectorAll('input[name=\"paymentMethod\"]');

// ==================== CONSTANTS ====================
const PLANO_ID = 1; // ID fixo do plano único
const PLANO_VALOR = 49.90;

// ==================== LOADING ====================
function showLoading() {
    loadingOverlay.style.display = 'flex';
}

function hideLoading() {
    loadingOverlay.style.display = 'none';
}

// ==================== ALERT SYSTEM ====================
function showAlert(message, type = 'error') {
    alertContainer.innerHTML = `
        <div class=\"alert alert-${type}\">
            ${message}
        </div>
    `;

    // Auto hide after 5 seconds
    setTimeout(() => {
        alertContainer.innerHTML = '';
    }, 5000);
}

// ==================== MODAL CONTROL ====================
function showCheckoutModal() {
    modal.style.display = 'flex';
    document.body.style.overflow = 'hidden';
}

function closeCheckoutModal() {
    modal.style.display = 'none';
    document.body.style.overflow = 'auto';
}

// Close modal on background click
modal.addEventListener('click', (e) => {
    if (e.target === modal) {
        closeCheckoutModal();
    }
});

// ==================== PAYMENT METHOD CHANGE ====================
paymentMethodRadios.forEach(radio => {
    radio.addEventListener('change', (e) => {
        const method = e.target.value;

        if (method === 'CREDIT_CARD') {
            creditCardFields.classList.remove('hidden');
            // Make credit card fields required
            document.getElementById('nomeNoCartao').required = true;
            document.getElementById('numeroCartao').required = true;
            document.getElementById('validadeCartao').required = true;
            document.getElementById('cvv').required = true;
        } else {
            creditCardFields.classList.add('hidden');
            // Remove required from credit card fields
            document.getElementById('nomeNoCartao').required = false;
            document.getElementById('numeroCartao').required = false;
            document.getElementById('validadeCartao').required = false;
            document.getElementById('cvv').required = false;
        }
    });
});

// ==================== INPUT MASKS ====================
// CPF/CNPJ Mask
const documentoInput = document.getElementById('documentoCliente');
documentoInput.addEventListener('input', (e) => {
    let value = e.target.value.replace(/\D/g, '');

    if (value.length <= 11) {
        // CPF: 000.000.000-00
        value = value.replace(/(\d{3})(\d)/, '$1.$2');
        value = value.replace(/(\d{3})(\d)/, '$1.$2');
        value = value.replace(/(\d{3})(\d{1,2})$/, '$1-$2');
    } else {
        // CNPJ: 00.000.000/0000-00
        value = value.slice(0, 14);
        value = value.replace(/^(\d{2})(\d)/, '$1.$2');
        value = value.replace(/^(\d{2})\.(\d{3})(\d)/, '$1.$2.$3');
        value = value.replace(/\.(\d{3})(\d)/, '.$1/$2');
        value = value.replace(/(\d{4})(\d)/, '$1-$2');
    }

    e.target.value = value;
});

// Credit Card Number Mask
const numeroCartaoInput = document.getElementById('numeroCartao');
if (numeroCartaoInput) {
    numeroCartaoInput.addEventListener('input', (e) => {
        let value = e.target.value.replace(/\D/g, '');
        value = value.replace(/(\d{4})(?=\d)/g, '$1 ');
        e.target.value = value.slice(0, 19);
    });
}

// Card Expiry Mask
const validadeInput = document.getElementById('validadeCartao');
if (validadeInput) {
    validadeInput.addEventListener('input', (e) => {
        let value = e.target.value.replace(/\D/g, '');
        if (value.length >= 2) {
            value = value.slice(0, 2) + '/' + value.slice(2, 4);
        }
        e.target.value = value;
    });
}

// CVV - only numbers
const cvvInput = document.getElementById('cvv');
if (cvvInput) {
    cvvInput.addEventListener('input', (e) => {
        e.target.value = e.target.value.replace(/\D/g, '').slice(0, 4);
    });
}

// ==================== FORM SUBMISSION ====================
checkoutForm.addEventListener('submit', async (e) => {
    e.preventDefault();

    const formData = new FormData(checkoutForm);
    const paymentMethod = document.querySelector('input[name=\"paymentMethod\"]:checked').value;
    const documentoCliente = formData.get('documentoCliente').replace(/\D/g, '');

    // Validate document
    if (documentoCliente.length !== 11 && documentoCliente.length !== 14) {
        showAlert('CPF ou CNPJ inválido. Verifique o número digitado.', 'error');
        return;
    }

    // Build request payload
    const payload = {
        planoId: PLANO_ID,
        formaPagamento: paymentMethod,
        documentoCliente: documentoCliente
    };

    // Add credit card data if payment method is CREDIT_CARD
    if (paymentMethod === 'CREDIT_CARD') {
        const numeroCartao = formData.get('numeroCartao').replace(/\D/g, '');
        const nomeNoCartao = formData.get('nomeNoCartao');
        const validadeCartao = formData.get('validadeCartao');
        const cvv = formData.get('cvv');

        // Validate credit card fields
        if (!numeroCartao || !nomeNoCartao || !validadeCartao || !cvv) {
            showAlert('Preencha todos os dados do cartão de crédito.', 'error');
            return;
        }

        // For now, we'll use a mock token (in production, use a tokenization service)
        payload.cartao = {
            tokenCartao: `tok_${numeroCartao.slice(-4)}_${Date.now()}`,
            nomeNoCartao: nomeNoCartao
        };
    }

    showLoading();

    try {
        const response = await fetch('/api/v1/assinaturas/contratar', {
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
            throw new Error(errorData.mensagem || 'Erro ao processar pagamento');
        }

        const data = await response.json();

        // Store subscription data for success page
        sessionStorage.setItem('assinaturaData', JSON.stringify(data));

        // Redirect to success page
        window.location.href = '/sucesso';

    } catch (error) {
        console.error('Erro ao contratar plano:', error);
        showAlert(error.message || 'Erro ao processar pagamento. Tente novamente.', 'error');
    } finally {
        hideLoading();
    }
});

// ==================== PAGE LOAD ====================
document.addEventListener('DOMContentLoaded', () => {
    console.log('Página de planos carregada');
});
"