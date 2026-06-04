/**
 * Taskflow Core API — Enterprise v2
 * Autenticação centralizada, interceptação de requests, gerenciamento de sessão.
 */
(function () {
    'use strict';

    let _loggingOut = false;

    function getCFG() {
        return window.TASKFLOW_CONFIG || {
            API_BASE_URL: '',
            TOKEN_KEY: 'taskflow_auth_token_v1',
            USER_KEY: 'taskflow_user_data_v1',
            ROUTES: { LOGIN: '/login' }
        };
    }

    /* ── TOKEN ── */
    function getToken() {
        const cfg = getCFG();
        return localStorage.getItem(cfg.TOKEN_KEY)
            || localStorage.getItem('accessToken')
            || null;
    }

    function setToken(token) {
        if (!token) return;
        const cfg = getCFG();
        localStorage.setItem(cfg.TOKEN_KEY, token);
        localStorage.removeItem('accessToken'); // normaliza

        const tenantId = decodeTenantId(token);
        if (tenantId) {
            localStorage.setItem('empresaId', tenantId);
            localStorage.setItem('tenantId', tenantId);
        }
    }

    function clearSession() {
        const cfg = getCFG();
        [cfg.TOKEN_KEY, 'accessToken', 'empresaId', 'tenantId', 'taskflow_token', cfg.USER_KEY]
            .forEach(k => localStorage.removeItem(k));
        sessionStorage.clear();
    }

    /* ── JWT UTILS ── */
    function decodeTenantId(token) {
        try {
            if (!token || token.split('.').length !== 3) return null;
            const payload = JSON.parse(atob(token.split('.')[1]));
            return payload.empresaId || payload.tenantId || null;
        } catch (_) { return null; }
    }

    function isTokenExpired(token) {
        try {
            if (!token) return true;
            const parts = token.split('.');
            if (parts.length !== 3) return true;
            const { exp } = JSON.parse(atob(parts[1]));
            return Date.now() / 1000 >= exp - 10;
        } catch (_) { return true; }
    }

    /* ── FETCH ── */
    async function apiFetch(path, options = {}) {
        const cfg = getCFG();
        const token = getToken();

        if (token && isTokenExpired(token)) {
            _handleAuthError();
            throw Object.assign(new Error('Sessão expirada'), { status: 401 });
        }

        const headers = new Headers({
            Accept: 'application/json',
            ...(options.body ? { 'Content-Type': 'application/json' } : {}),
            ...(options.headers || {})
        });

        if (token) {
            headers.set('Authorization', `Bearer ${token}`);
            const tenantId = localStorage.getItem('empresaId') || decodeTenantId(token);
            if (tenantId) headers.set('X-Tenant-ID', tenantId);
        }

        const url = path.startsWith('http') ? path : `${cfg.API_BASE_URL}${path}`;

        const response = await fetch(url, { ...options, headers });

        if (response.status === 401) {
            _handleAuthError();
            throw Object.assign(new Error('Não autorizado'), { status: 401 });
        }

        if (response.status === 204) return null;

        const data = await response.json().catch(() => ({}));

        if (!response.ok) {
            const err = new Error(data.mensagem || data.message || `Erro ${response.status}`);
            err.status = response.status;
            err.data = data;
            throw err;
        }

        return data;
    }

    function _handleAuthError() {
        if (_loggingOut) return;
        _loggingOut = true;
        const cfg = getCFG();
        clearSession();
        window.location.href = `${cfg.ROUTES.LOGIN}?expired=true`;
    }

    /* ── GUARD ── */
    function requireAuth() {
        const token = getToken();
        if (!token || isTokenExpired(token)) {
            _handleAuthError();
            return false;
        }
        return true;
    }

    /* ── PUBLIC API ── */
    window.Taskflow = {
        getToken,
        setToken,
        clearSession,
        apiFetch,
        decodeTenantId,
        requireAuth,
        api: {
            contratarPlano:     (p) => apiFetch('/api/v1/assinaturas/contratar',  { method: 'POST',   body: JSON.stringify(p) }),
            meuPlano:           ()  => apiFetch('/api/v1/assinaturas/meu-plano',  { method: 'GET'  }),
            cancelarAssinatura: ()  => apiFetch('/api/v1/assinaturas/cancelar',   { method: 'DELETE' }),
            buscarSegmentos:    ()  => apiFetch('/api/v1/empresas/segmentos',      { method: 'GET'  }),
            consultarCnpj:      (cnpj) => apiFetch(`/api/v1/empresas/consultar-cnpj/${cnpj}`, { method: 'GET' })
        }
    };
})();