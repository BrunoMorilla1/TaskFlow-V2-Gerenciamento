/**
 * Taskflow Success/Payment Control - Enterprise Version
 * Gerencia a confirmação de pagamento e sincronização com o Gateway.
 */
(function () {
    'use strict';

    const CFG = window.TASKFLOW_CONFIG;

    // Garantia de autenticação antes de renderizar dados sensíveis
    if (!window.Taskflow?.requireAuth()) return;

    // ==================== ELEMENTOS DO DOM ====================
    const el = {
        alert:              document.getElementById('alertContainer'),
        loading:            document.getElementById('loadingOverlay'),
        subHeadline:        document.getElementById('subHeadline'),
        statusBadge:        document.getElementById('statusBadge'),
        infoAssinaturaId:   document.getElementById('infoAssinaturaId'),
        infoFormaPagamento: document.getElementById('infoFormaPagamento'),
        infoValor:          document.getElementById('infoValor'),
        infoProximo:        document.getElementById('infoProximo'),
        paymentLinkBox:     document.getElementById('paymentLinkBox'),
        paymentLink:        document.getElementById('paymentLink'),
        paymentLinkTitle:   document.getElementById('paymentLinkTitle'),
        paymentLinkHint:    document.getElementById('paymentLinkHint'),
        btnAtualizar:       document.getElementById('btnAtualizar'),
        btnDashboard:       document.getElementById('btnDashboard'),
        btnLogout:          document.getElementById('btnLogout')
    };

    // Controle de Polling (Verificação automática)
    let pollingInterval = null;
    const POLLING_TIME = 5000; // 5 segundos

    // ==================== CONSTANTES DE NEGÓCIO ====================
    const FORMA_LABEL = {
        PIX: 'PIX (Instantâneo)',
        BOLETO: 'Boleto Bancário',
        CREDITO: 'Cartão de Crédito'
    };

    const STATUS_MAP = {
        PENDENTE:         { label: 'Aguardando Pagamento', class: 'status-pending' },
        EM_PROCESSAMENTO: { label: 'Processando...',       class: 'status-processing' },
        CONFIRMADO:       { label: 'Pagamento Aprovado',   class: 'status-paid' },
        RECEBIDO:         { label: 'Pagamento Recebido',   class: 'status-paid' },
        VENCIDO:          { label: 'Fatura Vencida',       class: 'status-overdue' },
        FALHA_CARTAO:     { label: 'Erro no Cartão',       class: 'status-error' },
        CANCELADO:        { label: 'Assinatura Cancelada', class: 'status-error' }
    };

    // ==================== UI HELPERS ====================

    function showAlert(msg, type = 'info') {
        if (!el.alert) return;
        el.alert.innerHTML = `<div class="alert alert-${type} show animate-in">${msg}</div>`;
        setTimeout(() => { el.alert.innerHTML = ''; }, 4000);
    }

    const formatMoney = (v) => Number(v || 0).toLocaleString('pt-BR', { style: 'currency', currency: 'BRL' });

    // ==================== CORE ENGINE ====================

    function render(data) {
        if (!data) return false;

        // Atualiza campos básicos
        el.infoAssinaturaId.textContent   = data.assinaturaId || data.asaasId || '—';
        el.infoFormaPagamento.textContent = FORMA_LABEL[data.formaPagamento] || data.formaPagamento;
        el.infoValor.textContent          = formatMoney(data.valor);

        // Lógica de Status
        const statusCfg = STATUS_MAP[data.status] || { label: data.status, class: 'status-default' };
        el.statusBadge.className = `status-badge ${statusCfg.class}`;
        el.statusBadge.textContent = statusCfg.label;

        const isPaid = ['CONFIRMADO', 'RECEBIDO'].includes(data.status);

        if (isPaid) {
            el.subHeadline.textContent = 'Parabéns! Sua conta Taskflow está ativa.';
            el.infoProximo.textContent = 'Ir para o Painel';
            stopPolling(); // Interrompe verificação se já pagou
        }

        // Gestão de Links Externos (Boleto/Pix/Fatura)
        const link = data.invoiceUrl || data.urlPagamento;
        if (link && !isPaid) {
            el.paymentLink.href = link;
            el.paymentLink.textContent = 'Clique aqui para abrir a fatura';
            el.paymentLinkBox.classList.remove('hidden');

            if (data.formaPagamento === 'BOLETO') el.paymentLinkTitle.textContent = 'Visualizar Boleto';
            if (data.formaPagamento === 'PIX') el.paymentLinkTitle.textContent = 'QR Code / Copia e Cola';
        } else {
            el.paymentLinkBox.classList.add('hidden');
        }

        return isPaid;
    }

    // ==================== API ACTIONS ====================

    async function sincronizarStatus(isSilent = false) {
        if (!isSilent) el.loading?.classList.add('show');

        try {
            // O Taskflow.api.meuPlano() já injeta o Token e X-Tenant-ID via api.js
            const data = await window.Taskflow.api.meuPlano();
            const paid = render(data);

            if (paid && !isSilent) {
                showAlert('Pagamento identificado! Redirecionando...', 'success');
                setTimeout(() => window.location.href = CFG.ROUTES.DASHBOARD, 1500);
            }
        } catch (err) {
            console.error('Falha ao sincronizar:', err);
            if (!isSilent) showAlert(err.message || 'Erro ao conectar com o servidor.');
        } finally {
            if (!isSilent) el.loading?.classList.remove('show');
        }
    }

    // ==================== POLLING SYSTEM ====================

    function startPolling() {
        if (pollingInterval) return;
        console.log('Taskflow: Iniciando monitoramento de pagamento...');
        pollingInterval = setInterval(() => sincronizarStatus(true), POLLING_TIME);
    }

    function stopPolling() {
        if (pollingInterval) {
            clearInterval(pollingInterval);
            pollingInterval = null;
        }
    }

    // ==================== INITIALIZATION ====================

    document.addEventListener('DOMContentLoaded', () => {
        // 1. Tenta carregar dados imediatos do sessionStorage (cache do checkout)
        try {
            const cached = sessionStorage.getItem('taskflow_assinatura');
            if (cached) {
                const data = JSON.parse(cached);
                const alreadyPaid = render(data);
                if (!alreadyPaid) startPolling(); // Se não pagou, começa a vigiar
            } else {
                // Se não tem cache, faz a primeira busca forçada
                sincronizarStatus().then(() => startPolling());
            }
        } catch (e) {
            sincronizarStatus().then(() => startPolling());
        }

        // 2. Event Listeners
        el.btnAtualizar?.addEventListener('click', () => sincronizarStatus(false));

        el.btnDashboard?.addEventListener('click', () => {
            window.location.href = CFG.ROUTES.DASHBOARD;
        });

        el.btnLogout?.addEventListener('click', (e) => {
            e.preventDefault();
            stopPolling();
            window.Taskflow.clearSession();
            window.location.href = CFG.ROUTES.LOGIN;
        });
    });

    // Limpeza ao sair da página
    window.addEventListener('beforeunload', stopPolling);

})();