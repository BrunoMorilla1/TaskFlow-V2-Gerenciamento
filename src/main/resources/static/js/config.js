(function (global) {
    'use strict';
    if (global.TASKFLOW_CONFIG) return;

    const CONFIG = Object.freeze({
        API_BASE_URL: (window.location.hostname === 'localhost' || window.location.hostname === '127.0.0.1')
            ? 'http://localhost:8080'
            : '',
        TOKEN_KEY:  'taskflow_auth_token_v1',
        USER_KEY:   'taskflow_user_data_v1',
        ROUTES: Object.freeze({
            LOGIN:     '/login',
            PLANOS:    '/planos',
            SUCESSO:   '/sucesso',
            DASHBOARD: '/dashboard',
            EMPRESA:   '/empresa',
            HOME:      '/'
        })
    });

    Object.defineProperty(global, 'TASKFLOW_CONFIG', {
        value: CONFIG,
        writable: false,
        configurable: false
    });
})(typeof window !== 'undefined' ? window : this);