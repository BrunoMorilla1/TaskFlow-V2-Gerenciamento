(function () {
    const CFG = window.TASKFLOW_CONFIG;
    if (!window.Taskflow?.requireAuth?.()) return;

    const el = {
        alert: document.getElementById('alertContainer'),
        loading: document.getElementById('loadingOverlay'),
        cardAssinatura: document.querySelector('[data-testid=\"card-assinatura\"]'),
        semAssinatura:  document.getElementById('semAssinaturaCard'),
        statusBadge:    document.getElementById('statusBadge'),
        statusMessage:  document.getElementById('statusMessage'),
        infoAsaasId:    document.getElementById('infoAsaasId'),
        infoValor:      document.getElementById('infoValor'),
        infoDataPagamento:  document.getElementById('infoDataPagamento'),
        infoDataVencimento: document.getElementById('infoDataVencimento'),
        paymentLinkBox: document.getElementById('paymentLinkBox'),
        paymentLink:    document.getElementById('paymentLink'),
        btnAtualizar:   document.getElementById('btnAtualizar'),
        btnCancelar:    document.getElementById('btnCancelar'),
        btnLogout:      document.getElementById('btnLogout'),
        userEmail:      document.getElementById('userEmail')
    };

    const STATUS_LABEL = {
        PENDENTE: 'Aguardando pagamento',
        EM_PROCESSAMENTO: 'Em processamento',
        CONFIRMADO: 'Pagamento confirmado',
        RECEBIDO: 'Pagamento recebido',
        VENCIDO: 'Pagamento vencido',
        ESTORNADO: 'Estornado',
        CANCELADO: 'Cancelado',
        FALHA_CARTAO: 'Falha no cartão',
        INDEFINIDO: 'Status desconhecido'
    };

    function showAlert(msg, type = 'error') {
        el.alert.innerHTML = `<div class=\"alert alert-${type}\">${msg}</div>`;
        setTimeout(() => { el.alert.innerHTML = ''; }, 5000);
    }
    const showLoading = () => el.loading.classList.add('show');
    const hideLoading = () => el.loading.classList.remove('show');

    function formatMoney(v) {
        const n = typeof v === 'number' ? v : parseFloat(v);
        if (isNaN(n)) return '—';
        return n.toLocaleString('pt-BR', { style: 'currency', currency: 'BRL' });
    }
    function formatDate(value) {
        if (!value) return '—';
        try {
            const d = new Date(value);
            if (isNaN(d.getTime())) return value;
            return d.toLocaleDateString('pt-BR');
        } catch (_) { return value; }
    }

    function renderEmpty() {
        el.cardAssinatura.classList.add('hidden');
        el.semAssinatura.classList.remove('hidden');
    }

    function renderStatus(data) {
        el.cardAssinatura.classList.remove('hidden');
        el.semAssinatura.classList.add('hidden');

        const status = data.status || 'INDEFINIDO';
        el.statusBadge.className = `status-badge status-${status}`;
        el.statusBadge.textContent = STATUS_LABEL[status] || status;
        el.statusMessage.textContent = data.mensagem || '';

        el.infoAsaasId.textContent        = data.asaasId || '—';
        el.infoValor.textContent          = formatMoney(data.valor);
        el.infoDataPagamento.textContent  = formatDate(data.dataPagamento);
        el.infoDataVencimento.textContent = formatDate(data.dataVencimento);

        if (data.urlPagamento) {
            el.paymentLink.href = data.urlPagamento;
            el.paymentLink.textContent = data.urlPagamento;
            el.paymentLinkBox.classList.remove('hidden');
        } else {
            el.paymentLinkBox.classList.add('hidden');
        }

        // Bloqueia cancelamento se já cancelada/estornada
        const jaFinalizada = status === 'CANCELADO' || status === 'ESTORNADO';
        el.btnCancelar.disabled = jaFinalizada;
        el.btnCancelar.style.display = jaFinalizada ? 'none' : '';
    }

    async function carregarStatus(silent) {
        if (!silent) showLoading();
        try {
            const data = await window.Taskflow.api.meuPlano();
            renderStatus(data);
        } catch (error) {
            if (error.status === 404 || /nenhuma transa/i.test(error.message || '')) {
                renderEmpty();
            } else {
                showAlert(error.message || 'Não foi possível carregar sua assinatura.', 'error');
            }
        } finally {
            if (!silent) hideLoading();
        }
    }

    async function cancelarAssinatura() {
        const ok = window.confirm('Tem certeza que deseja cancelar sua assinatura? Essa ação é irreversível.');
        if (!ok) return;

        showLoading();
        try {
            await window.Taskflow.api.cancelarAssinatura();
            showAlert('Assinatura cancelada com sucesso.', 'success');
            await carregarStatus(true);
        } catch (error) {
            showAlert(error.message || 'Não foi possível cancelar a assinatura.', 'error');
        } finally {
            hideLoading();
        }
    }

    el.btnAtualizar.addEventListener('click', () => carregarStatus(false));
    el.btnCancelar.addEventListener('click', cancelarAssinatura);
    el.btnLogout.addEventListener('click', (e) => {
        e.preventDefault();
        window.Taskflow.clearSession();
        window.location.href = CFG.ROUTES.LOGIN;
    });

    document.addEventListener('DOMContentLoaded', () => {
        try {
            const user = JSON.parse(localStorage.getItem(CFG.USER_KEY) || 'null');
            if (user?.email) el.userEmail.textContent = user.email;
        } catch (_) { /* ignore */ }
        carregarStatus(false);
    });
})();
