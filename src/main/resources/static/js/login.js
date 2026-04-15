/**
 * TASKFLOW - AUTH LOGIC (PRO VERSION)
 * Gerencia a autenticação, estados da UI e integração com a API Spring Boot.
 */

'use strict';

document.addEventListener('DOMContentLoaded', () => {

    // Seletores Principais
    const container = document.getElementById('authContainer');
    const authForm = document.getElementById('authForm');
    const feedback = document.getElementById('authFeedback');

    // Elementos Dinâmicos da UI
    const formTitle = document.getElementById('formTitle');
    const formSubtitle = document.getElementById('formSubtitle');
    const submitBtn = document.getElementById('submitBtn');
    const visualTitle = document.getElementById('visualTitle');
    const visualText = document.getElementById('visualText');
    const switchBtn = document.getElementById('switchBtn');
    const linkForgot = document.getElementById('forgotPass'); // Seletor para esqueci senha

    // Inputs
    const groupNome = document.getElementById('groupNome');
    const inputNome = document.getElementById('nome');
    const inputEmail = document.getElementById('email');
    const inputSenha = document.getElementById('senha');

    let isSignup = false;

    /**
     * Alterna entre os modos Login e Cadastro
     */
    window.toggleAuth = () => {
        isSignup = !isSignup;

        container.classList.toggle('is-signup');
        hideFeedback();
        authForm.reset();

        if (isSignup) {
            updateUI(
                "Crie sua conta",
                "Preencha os dados abaixo para começar.",
                "Cadastrar Agora",
                "Já é de casa?",
                "Acesse sua conta para gerenciar seus resultados.",
                "Fazer Login",
                true
            );
        } else {
            updateUI(
                "Bem-vindo!",
                "Insira suas credenciais de acesso.",
                "Acessar Plataforma",
                "Novo por aqui?",
                "Crie sua conta agora e comece a organizar sua empresa com inteligência.",
                "Criar Conta Grátis",
                false
            );
        }
    };

    function updateUI(fTitle, fSub, sBtn, vTitle, vText, swBtn, showNome) {
        formTitle.textContent = fTitle;
        formSubtitle.textContent = fSub;
        submitBtn.innerHTML = `<span>${sBtn}</span>`;
        visualTitle.textContent = vTitle;
        visualText.textContent = vText;
        switchBtn.textContent = swBtn;

        // Gerencia visibilidade do campo Nome e Link Esqueci Senha
        groupNome.style.display = showNome ? "block" : "none";
        linkForgot.style.visibility = showNome ? "hidden" : "visible"; // "arranca" no registro
        inputNome.required = showNome;
    }

    /**
     * Envio dos dados para o Spring Boot
     */
    authForm.addEventListener('submit', async (e) => {
        e.preventDefault();
        hideFeedback();

        const emailValue = inputEmail.value.trim().toLowerCase();
        const senhaValue = inputSenha.value;
        const nomeValue = inputNome.value.trim();

        if (!emailValue || !senhaValue || (isSignup && !nomeValue)) {
            showFeedback("Por favor, preencha todos os campos obrigatórios.", "error");
            return;
        }

        setLoading(true);

        const url = isSignup ? '/api/v1/usuarios' : '/api/v1/auth/login';
        const payload = { email: emailValue, senha: senhaValue };
        if (isSignup) payload.nome = nomeValue;

        try {
            const response = await fetch(url, {
                method: 'POST',
                headers: {
                    'Content-Type': 'application/json',
                    'Accept': 'application/json'
                },
                body: JSON.stringify(payload)
            });

            // CORREÇÃO DO LOG: Tenta extrair a mensagem do corpo antes de verificar response.ok
            let data = {};
            try {
                data = await response.json();
            } catch (pErr) { /* Resposta não é JSON */ }

            if (response.ok) {
                handleSuccess(data);
            } else {
                // Se o Java enviou EmailJaCadastradoException, pegamos a mensagem exata
                // Prioriza 'mensagem' (seu DTO) depois 'message' (padrão Spring)
                const errorMsg = data.mensagem || data.message || "Credenciais inválidas ou erro no servidor.";
                throw new Error(errorMsg);
            }
        } catch (err) {
            console.error("Auth Error:", err);
            showFeedback(err.message, "error");
        } finally {
            setLoading(false);
        }
    });

 function handleSuccess(data) {
     if (isSignup) {
         showFeedback("Conta criada com sucesso! Redirecionando...", "success");
         setTimeout(() => window.toggleAuth(), 1500);
     } else {
         const token = data.accessToken || data.token;

         if (token) {
             localStorage.setItem('token', token);

             showFeedback("Acesso autorizado! Carregando...", "success");

             setTimeout(() => {
                 window.location.href = '/empresa';
             }, 800);
         }
     }
 }

    function setLoading(state) {
        submitBtn.disabled = state;
        submitBtn.innerHTML = state ? `Processando...` : `<span>${isSignup ? "Cadastrar Agora" : "Acessar Plataforma"}</span>`;
    }

    function showFeedback(msg, type) {
        feedback.textContent = msg;
        feedback.className = `feedback-toast ${type === "error" ? "error-msg" : "success-msg"}`;
        feedback.style.display = "block";

        if (type === "error") {
            container.classList.add('shake');
            setTimeout(() => container.classList.remove('shake'), 400);
        }
    }

    function hideFeedback() {
        feedback.style.display = "none";
    }
});