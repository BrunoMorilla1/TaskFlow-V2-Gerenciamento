"// ==================== ELEMENTS ====================
const statusValue = document.getElementById('statusValue');
const assinaturaIdElement = document.getElementById('assinaturaId');
const paymentLinkBox = document.getElementById('paymentLinkBox');
const paymentLink = document.getElementById('paymentLink');

// ==================== STATUS MAPPING ====================
const STATUS_MAP = {
    'PENDENTE': {
        text: 'Aguardando pagamento',
        color: '#f59e0b'
    },
    'CONFIRMADO': {
        text: 'Pagamento confirmado',
        color: '#10b981'
    },
    'RECEBIDO': {
        text: 'Pagamento recebido',
        color: '#10b981'
    },
    'VENCIDO': {
        text: 'Pagamento vencido',
        color: '#ef4444'
    },
    'CANCELADO': {
        text: 'Assinatura cancelada',
        color: '#64748b'
    }
};

// ==================== LOAD SUBSCRIPTION DATA ====================
function loadSubscriptionData() {
    try {
        // Get data from sessionStorage (set by planos.js)
        const dataStr = sessionStorage.getItem('assinaturaData');

        if (!dataStr) {
            console.warn('Nenhum dado de assinatura encontrado');
            return;
        }

        const data = JSON.parse(dataStr);

        // Update assinatura ID
        if (data.assinaturaId) {
            assinaturaIdElement.textContent = data.assinaturaId;
        }

        // Update status
        if (data.status) {
            const statusInfo = STATUS_MAP[data.status] || {
                text: data.status,
                color: '#64748b'
            };

            statusValue.textContent = statusInfo.text;
            statusValue.style.color = statusInfo.color;
        }

        // Show payment link if available
        if (data.invoiceUrl || data.boletoUrl || data.pixQrCode) {
            const url = data.invoiceUrl || data.boletoUrl || data.pixQrCode;
            paymentLink.href = url;
            paymentLinkBox.classList.remove('hidden');
        }

        // Clear sessionStorage after loading
        sessionStorage.removeItem('assinaturaData');

    } catch (error) {
        console.error('Erro ao carregar dados da assinatura:', error);
    }
}

// ==================== CHECK SUBSCRIPTION STATUS ====================
async function checkSubscriptionStatus() {
    try {
        const response = await fetch('/api/v1/assinaturas/meu-plano', {
            method: 'GET',
            headers: {
                'Accept': 'application/json'
            },
            credentials: 'include'
        });

        if (!response.ok) {
            console.warn('Não foi possível verificar status da assinatura');
            return;
        }

        const data = await response.json();

        // Update status with latest info
        if (data.status) {
            const statusInfo = STATUS_MAP[data.status] || {
                text: data.status,
                color: '#64748b'
            };

            statusValue.textContent = statusInfo.text;
            statusValue.style.color = statusInfo.color;
        }

    } catch (error) {
        console.error('Erro ao verificar status:', error);
    }
}

// ==================== PAGE LOAD ====================
document.addEventListener('DOMContentLoaded', () => {
    loadSubscriptionData();

    // Check status after 3 seconds (in case payment was instant)
    setTimeout(() => {
        checkSubscriptionStatus();
    }, 3000);
});
"