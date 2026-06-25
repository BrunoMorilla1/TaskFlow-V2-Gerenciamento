const api = {
    getToken() {
        return localStorage.getItem('token');
    },

    getHeaders() {
        return {
            'Content-Type': 'application/json',
            'Authorization': `Bearer ${this.getToken()}`
        };
    },

    async request(url, options = {}) {
        const controller = new AbortController();
        const timeout = setTimeout(() => controller.abort(), config.REQUEST_TIMEOUT);

        try {
            const response = await fetch(config.API_BASE_URL + url, {
                ...options,
                headers: this.getHeaders(),
                signal: controller.signal
            });

            clearTimeout(timeout);
            return this.handleResponse(response);
        } catch (error) {
            clearTimeout(timeout);
            if (error.name === 'AbortError') {
                throw new Error('Requisição timeout');
            }
            throw error;
        }
    },

    async handleResponse(response) {
        if (response.status === 401) {
            localStorage.removeItem('token');
            window.location.href = 'index.html';
            throw new Error('Sessão expirada');
        }

        if (!response.ok) {
            const errorData = await response.json().catch(() => ({}));
            throw new Error(errorData.message || 'Erro na requisição');
        }

        return response.json();
    },

    async get(url) {
        return this.request(url, { method: 'GET' });
    },

    async post(url, data) {
        return this.request(url, {
            method: 'POST',
            body: JSON.stringify(data)
        });
    },

    async put(url, data) {
        return this.request(url, {
            method: 'PUT',
            body: JSON.stringify(data)
        });
    },

    async delete(url) {
        return this.request(url, { method: 'DELETE' });
    }
};

export default api;