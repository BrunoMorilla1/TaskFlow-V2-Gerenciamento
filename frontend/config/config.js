const config = {
    ENV: 'development',

    API_BASE_URL: 'http://localhost:8080/api',

    TOKEN_EXPIRATION_MINUTES: 60,

    APP_NAME: 'TaskFlow Gerenciamento',
    APP_VERSION: '2.0.0',

    REQUEST_TIMEOUT: 15000,

    DEFAULT_PAGE_SIZE: 15,

    PUBLIC_PATHS: [
        '/login',
        '/index.html',
        '/'
    ],

    ENDPOINTS: {
        AUTH: {
            LOGIN: '/auth/login',
            LOGOUT: '/auth/logout',
            ME: '/auth/me'
        },
        EMPRESAS: {
            BASE: '/empresas',
            BY_ID: '/empresas/:id'
        },
        CLIENTES: {
            BASE: '/clientes',
            BY_ID: '/clientes/:id'
        },
        FUNCIONARIOS: {
            BASE: '/funcionarios',
            BY_ID: '/funcionarios/:id'
        },
        PRODUTOS: {
            BASE: '/produtos',
            BY_ID: '/produtos/:id'
        },
        ORDENS_SERVICO: {
            BASE: '/ordens-servico',
            BY_ID: '/ordens-servico/:id'
        },
        ORCAMENTOS: {
            BASE: '/orcamentos',
            BY_ID: '/orcamentos/:id',
            PDF: '/orcamentos/:id/pdf'
        },
        FINANCEIRO: {
            LANCAMENTOS: '/financeiro/lancamentos',
            PAGAMENTOS: '/financeiro/pagamentos',
            RELATORIOS: '/financeiro/relatorios'
        }
    }
};

export function buildUrl(endpoint) {
    return config.API_BASE_URL + endpoint;
}

export default config;