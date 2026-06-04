'use strict';

/**
 * Taskflow Auth Control - Enterprise Version
 */
document.addEventListener('DOMContentLoaded', () => {

    // FUNÇÃO AUXILIAR: Busca a config de forma segura para evitar o erro de "undefined"
    const getSafeConfig = () => {
        return window.TASKFLOW_CONFIG || {
            API_BASE_URL: "http://localhost:8080",
            ROUTES: { LOGIN: "/login", PLANOS: "/planos" }
        };
    };

    // ==================== ELEMENTOS DO DOM ====================
    const el = {
        container:    document.getElementById('authContainer'),
        authForm:     document.getElementById('authForm'),
        feedback:     document.getElementById('authFeedback'),
        formTitle:    document.getElementById('formTitle'),
        formSubtitle: document.getElementById('formSubtitle'),
        submitBtn:    document.getElementById('submitBtn'),
        visualTitle:  document.getElementById('visualTitle'),
        visualText:   document.getElementById('visualText'),
        switchBtn:    document.getElementById('switchBtn'),
        linkForgot:   document.getElementById('forgotPass'),
        groupNome:    document.getElementById('groupNome'),
        inputNome:    document.getElementById('nome'),
        inputEmail:   document.getElementById('email'),
        inputSenha:   document.getElementById('senha')
    };

    if (!el.authForm) return; // Proteção caso o script carregue em página sem form

    let isSignup = false;

    // ==================== UI CONTROLLER ====================

    window.toggleAuth = () => {
        isSignup = !isSignup;
        el.container.classList.toggle('is-signup');
        hideFeedback();
        el.authForm.reset();

        const uiCfg = isSignup ? {
            title: "Crie sua conta",
            sub: "Preencha os dados abaixo para começar.",
            btn: "Cadastrar Agora",
            vTitle: "Já é de casa?",
            vText: "Acesse sua conta para gerenciar seus resultados.",
            vBtn: "Fazer Login"
        } : {
            title: "Bem-vindo!",
            sub: "Insira suas credenciais de acesso.",
            btn: "Acessar Plataforma",
            vTitle: "Novo por aqui?",
            vText: "Crie sua conta agora e organize sua empresa com inteligência.",
            vBtn: "Criar Conta Grátis"
        };

        updateUI(uiCfg);
    };

    function updateUI(cfg) {
        el.formTitle.textContent = cfg.title;
        el.formSubtitle.textContent = cfg.sub;
        el.submitBtn.innerHTML = `<span>${cfg.btn}</span>`;
        el.visualTitle.textContent = cfg.vTitle;
        el.visualText.textContent = cfg.vText;
        el.switchBtn.textContent = cfg.vBtn;

        el.groupNome.style.display = isSignup ? "block" : "none";
        el.linkForgot.style.visibility = isSignup ? "hidden" : "visible";
        el.inputNome.required = isSignup;
    }

    // ==================== LÓGICA DE NEGÓCIO ====================

    el.authForm.addEventListener('submit', async (e) => {
        e.preventDefault();
        hideFeedback();

        const config = getSafeConfig(); // Captura a config aqui dentro!
        const email = el.inputEmail.value.trim().toLowerCase();
        const senha = el.inputSenha.value;
        const nome  = el.inputNome.value.trim();

        if (!email || !senha || (isSignup && !nome)) {
            return showFeedback("Preencha todos os campos obrigatórios.", "error");
        }

        setLoading(true);

        const urlPath = isSignup ? '/api/v1/usuarios' : '/api/v1/auth/login';

        try {
            // CORREÇÃO AQUI: Usando a URL da config de forma segura
            const response = await fetch(`${config.API_BASE_URL}${urlPath}`, {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify({ email, senha, ...(isSignup && { nome }) })
            });

            const data = await response.json().catch(() => ({}));

            if (!response.ok) {
                throw new Error(data.mensagem || data.message || 'Falha na autenticação');
            }

            handleSuccess(data);

        } catch (err) {
            showFeedback(err.message, "error");
            setLoading(false);
        }
    });

    function handleSuccess(data) {
        const config = getSafeConfig();

        if (isSignup) {
            showFeedback("Conta criada! Redirecionando para login...", "success");
            setTimeout(() => window.toggleAuth(), 1500);
            return;
        }

        const token = data.accessToken || data.token;
        if (!token) {
            showFeedback("Erro: Servidor não enviou o token.", "error");
            setLoading(false);
            return;
        }

        // Grava o Token e Tenant via Core API
        if (window.Taskflow) {
            window.Taskflow.setToken(token);
        }

        showFeedback("Autenticado com sucesso!", "success");

        setTimeout(() => {
            const tenantId = window.Taskflow ? window.Taskflow.decodeTenantId(token) : null;

            if (tenantId) {
                // CORREÇÃO AQUI: Acesso seguro à rota
                window.location.href = config.ROUTES.PLANOS;
            } else {
                window.location.href = '/empresa';
            }
        }, 1000);
    }

    // ==================== HELPERS ====================

    function setLoading(state) {
        el.submitBtn.disabled = state;
        el.submitBtn.innerHTML = state ?
            '<i class="spinner"></i> Processando...' :
            `<span>${isSignup ? "Cadastrar Agora" : "Acessar Plataforma"}</span>`;
    }

    function showFeedback(msg, type) {
        el.feedback.textContent = msg;
        el.feedback.className = `feedback-toast ${type === "error" ? "error-msg" : "success-msg"}`;
        el.feedback.style.display = "block";
    }

    function hideFeedback() {
        el.feedback.style.display = "none";
    }
});