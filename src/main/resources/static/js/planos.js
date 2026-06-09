(function () {
    'use strict';

    // ── CONFIG ────────────────────────────────────────────────────
    const CFG = window.TASKFLOW_CONFIG || {
        PLANO: { ID: 'PRO' },
        ROUTES: {
            SUCESSO:   '/sucesso.html',
            DASHBOARD: '/dashboard.html'
        }
    };

    // ── ELEMENTOS ─────────────────────────────────────────────────
    const el = {
        form:             document.getElementById('checkoutForm'),
        loadingOverlay:   document.getElementById('loadingOverlay'),
        alertContainer:   document.getElementById('alertContainer'),
        creditCardFields: document.getElementById('creditCardFields'),
        paymentRadios:    document.querySelectorAll('input[name="paymentMethod"]'),
        btnConfirmar:     document.getElementById('btnConfirmar'),
        btnText:          document.getElementById('btnText'),
        payNotice:        document.getElementById('payNotice'),
        docInput:         document.getElementById('documentoCliente'),
        cardBrandIcon:    document.getElementById('cardBrandIcon'),
        numeroCartao:     document.getElementById('numeroCartao')
    };

    // ── UTILS ─────────────────────────────────────────────────────
    const Utils = {
        onlyDigits: (str) => String(str || '').replace(/\D/g, ''),

        sanitize: (str) => String(str || '').trim(),

        validateCPF(cpf) {
            cpf = this.onlyDigits(cpf);
            if (cpf.length !== 11 || /^(\d)\1{10}$/.test(cpf)) return false;
            let sum = 0, r;
            for (let i = 0; i < 9; i++) sum += parseInt(cpf[i]) * (10 - i);
            r = sum % 11;
            if ((r < 2 ? 0 : 11 - r) !== parseInt(cpf[9])) return false;
            sum = 0;
            for (let i = 0; i < 10; i++) sum += parseInt(cpf[i]) * (11 - i);
            r = sum % 11;
            return (r < 2 ? 0 : 11 - r) === parseInt(cpf[10]);
        },

        validateCNPJ(cnpj) {
            cnpj = this.onlyDigits(cnpj);
            if (cnpj.length !== 14 || /^(\d)\1{13}$/.test(cnpj)) return false;
            const calc = (c, n) => {
                let s = 0, p = n - 7;
                for (let i = 0; i < n; i++) {
                    s += parseInt(c[i]) * (p > 1 ? p-- : (p = 9, p--));
                }
                const r = s % 11;
                return r < 2 ? 0 : 11 - r;
            };
            return calc(cnpj, 12) === parseInt(cnpj[12]) &&
                   calc(cnpj, 13) === parseInt(cnpj[13]);
        },

        validateDocument(raw) {
            const digits = this.onlyDigits(raw);
            if (digits.length === 11) return this.validateCPF(digits);
            if (digits.length === 14) return this.validateCNPJ(digits);
            return false;
        },

        detectCardBrand(number) {
            const n = this.onlyDigits(number);
            if (/^4/.test(n))               return { emoji: '💳', name: 'Visa' };
            if (/^5[1-5]|^2[2-7]/.test(n)) return { emoji: '💳', name: 'Mastercard' };
            if (/^3[47]/.test(n))           return { emoji: '💳', name: 'Amex' };
            if (/^(4011|5067|4576)/.test(n))return { emoji: '💳', name: 'Elo' };
            return null;
        },

        formatCardNumber(value) {
            return this.onlyDigits(value).slice(0, 16).replace(/(\d{4})/g, '$1 ').trim();
        }
    };

    // ── UI ────────────────────────────────────────────────────────
    const UI = {
        showLoading(msg = 'Processando…') {
            if (el.loadingOverlay) {
                el.loadingOverlay.querySelector('.loader-text').textContent = msg;
                el.loadingOverlay.hidden = false;
                el.loadingOverlay.classList.remove('hide');
            }
        },

        hideLoading() {
            if (el.loadingOverlay) {
                el.loadingOverlay.classList.add('hide');
                setTimeout(() => { el.loadingOverlay.hidden = true; }, 320);
            }
        },

        showAlert(message, type = 'error') {
            if (!el.alertContainer) return;
            const cls = type === 'success' ? 'alert-success' : 'alert-danger';
            const icon = type === 'success' ? '✓' : '⚠';
            el.alertContainer.innerHTML =
                `<div class="alert ${cls}" role="alert"><span>${icon}</span><span>${message}</span></div>`;
            const ttl = type === 'success' ? 8000 : 6000;
            setTimeout(() => { el.alertContainer.innerHTML = ''; }, ttl);
        },

        setButtonLoading(loading) {
            if (!el.btnConfirmar) return;
            el.btnConfirmar.disabled = loading;
            if (el.btnText) {
                el.btnText.textContent = loading ? 'Aguarde…' : 'Confirmar e Ativar Plano';
            }
        }
    };

    // ── PAYMENT HANDLER ───────────────────────────────────────────
    const Payment = {
        currentMethod: null,

        onMethodChange(method) {
            this.currentMethod = method;
            const isCard = method === 'CREDIT_CARD';

            if (el.creditCardFields) {
                el.creditCardFields.classList.toggle('hidden', !isCard);
                el.creditCardFields.querySelectorAll('input').forEach(input => {
                    input.required = isCard;
                    if (!isCard) input.value = '';
                });
            }

            if (el.payNotice) {
                if (method === 'BOLETO') {
                    el.payNotice.textContent = 'O boleto vence em até 3 dias úteis após a confirmação.';
                } else {
                    el.payNotice.textContent = '';
                }
            }
        },

        validate(formData) {
            const doc = Utils.onlyDigits(formData.get('documentoCliente'));
            if (!doc || !Utils.validateDocument(doc)) {
                UI.showAlert('CPF ou CNPJ inválido. Verifique o número informado.');
                return false;
            }

            const method = formData.get('paymentMethod');
            if (!method) {
                UI.showAlert('Selecione uma forma de pagamento.');
                return false;
            }

            if (method === 'CREDIT_CARD') {
                const nome   = Utils.sanitize(formData.get('nomeNoCartao'));
                const numero = Utils.onlyDigits(formData.get('numeroCartao'));
                const mes    = Utils.onlyDigits(formData.get('mesValidade'));
                const ano    = Utils.onlyDigits(formData.get('anoValidade'));
                const cvv    = Utils.onlyDigits(formData.get('cvv'));
                const cep    = Utils.onlyDigits(formData.get('cep'));
                const numero_end = Utils.sanitize(formData.get('numeroEndereco'));
                const tel    = Utils.onlyDigits(formData.get('telefone'));

                if (!nome) { UI.showAlert('Nome no cartão é obrigatório.'); return false; }
                if (numero.length < 13 || numero.length > 19) { UI.showAlert('Número do cartão inválido.'); return false; }
                if (!mes || mes.length > 2 || parseInt(mes) < 1 || parseInt(mes) > 12) { UI.showAlert('Mês de validade inválido.'); return false; }
                if (!ano || ano.length !== 4 || parseInt(ano) < new Date().getFullYear()) { UI.showAlert('Ano de validade inválido.'); return false; }
                if (!cvv || cvv.length < 3) { UI.showAlert('CVV inválido.'); return false; }
                if (!cep || cep.length !== 8) { UI.showAlert('CEP inválido (somente os 8 números).'); return false; }
                if (!numero_end) { UI.showAlert('Número do endereço é obrigatório.'); return false; }
                if (!tel || tel.length < 10) { UI.showAlert('Telefone inválido.'); return false; }
            }

            return true;
        },

        buildPayload(formData) {
            const method = formData.get('paymentMethod');
            const payload = {
                nomePlano:         CFG.PLANO?.ID || 'PRO',
                formaPagamento:    method,
                documentoCliente:  Utils.onlyDigits(formData.get('documentoCliente'))
            };

            if (method === 'CREDIT_CARD') {
                payload.cartao = {
                    nomeNoCartao:    Utils.sanitize(formData.get('nomeNoCartao')).toUpperCase(),
                    numeroCartao:    Utils.onlyDigits(formData.get('numeroCartao')),
                    mesValidade:     Utils.onlyDigits(formData.get('mesValidade')).padStart(2, '0'),
                    anoValidade:     Utils.onlyDigits(formData.get('anoValidade')),
                    cvv:             Utils.onlyDigits(formData.get('cvv')),
                    cep:             Utils.onlyDigits(formData.get('cep')),
                    numeroEndereco:  Utils.sanitize(formData.get('numeroEndereco')),
                    telefone:        Utils.onlyDigits(formData.get('telefone'))
                };
            }

            return payload;
        },

        async submit(e) {
            e.preventDefault();

            if (!window.Taskflow?.requireAuth?.()) {
                UI.showAlert('Você precisa estar autenticado para continuar.');
                return;
            }

            const formData = new FormData(el.form);
            if (!Payment.validate(formData)) return;

            const payload = Payment.buildPayload(formData);

            try {
                UI.showLoading('Processando assinatura…');
                UI.setButtonLoading(true);

                const response = await window.Taskflow.api.contratarPlano(payload);

                sessionStorage.setItem('taskflow_assinatura', JSON.stringify(response));
                UI.showAlert('Plano ativado com sucesso! Redirecionando…', 'success');

                setTimeout(() => {
                    window.location.href = CFG.ROUTES?.SUCESSO || '/sucesso.html';
                }, 1600);

            } catch (err) {
                console.error('[TaskFlow Checkout]', err);
                UI.showAlert(err?.message || 'Não foi possível processar o pagamento. Tente novamente.');
            } finally {
                UI.hideLoading();
                UI.setButtonLoading(false);
            }
        }
    };

    // ── INIT ──────────────────────────────────────────────────────
    async function init() {
        if (!el.form) {
            console.warn('[TaskFlow] Formulário de checkout não encontrado.');
            return;
        }

        // Verificar se usuário já possui plano ativo
        try {
            UI.showLoading('Verificando sua conta…');
            const plano = await window.Taskflow?.api?.meuPlano?.();
            const ativos = new Set(['CONFIRMADO', 'RECEBIDO', 'ATIVO', 'APPROVED']);
            if (plano?.status && ativos.has(plano.status.toUpperCase())) {
                window.location.href = CFG.ROUTES?.DASHBOARD || '/dashboard.html';
                return;
            }
        } catch {
            // Sem plano ativo — prosseguir normalmente
        } finally {
            UI.hideLoading();
        }

        // Listeners de forma de pagamento
        el.paymentRadios.forEach(radio => {
            radio.addEventListener('change', (e) => Payment.onMethodChange(e.target.value));
        });

        // Estado inicial com base no radio já marcado
        const checked = document.querySelector('input[name="paymentMethod"]:checked');
        if (checked) Payment.onMethodChange(checked.value);

        // Máscara de cartão + detecção de bandeira
        if (el.numeroCartao && el.cardBrandIcon) {
            el.numeroCartao.addEventListener('input', (e) => {
                const raw = Utils.onlyDigits(e.target.value).slice(0, 16);
                e.target.value = raw.replace(/(\d{4})(?=\d)/g, '$1 ');
                const brand = Utils.detectCardBrand(raw);
                el.cardBrandIcon.textContent = brand ? brand.emoji : '';
                el.cardBrandIcon.title = brand ? brand.name : '';
            });
        }

        // Submissão
        el.form.addEventListener('submit', Payment.submit);
    }

    // Bootstrap
    if (document.readyState === 'loading') {
        document.addEventListener('DOMContentLoaded', init);
    } else {
        init();
    }

})();
