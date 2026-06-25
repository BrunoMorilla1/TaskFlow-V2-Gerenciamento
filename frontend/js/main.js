let currentUser = null;

document.addEventListener('DOMContentLoaded', function() {
    checkAuth();
    initSidebar();
    initTheme();
});

function checkAuth() {
    const token = localStorage.getItem('token');
    const currentPage = window.location.pathname.split('/').pop();

    if (!token && !config.PUBLIC_PATHS.includes('/' + currentPage)) {
        window.location.href = 'index.html';
    }
}

function initSidebar() {
    const sidebar = document.getElementById('sidebar');
    if (sidebar) {
        const currentPath = window.location.pathname;
        const menuItems = sidebar.querySelectorAll('a');

        menuItems.forEach(item => {
            if (currentPath.includes(item.getAttribute('href'))) {
                item.classList.add('active');
            }
        });
    }
}

function initTheme() {
    // Dark mode por padrão (enterprise)
    document.documentElement.setAttribute('data-theme', 'dark');
}

function logout() {
    localStorage.removeItem('token');
    localStorage.removeItem('user');
    window.location.href = 'index.html';
}

function showToast(message, type = 'success') {
    const toast = document.createElement('div');
    toast.className = `toast toast-${type}`;
    toast.textContent = message;
    document.body.appendChild(toast);

    setTimeout(() => {
        toast.style.opacity = '0';
        setTimeout(() => toast.remove(), 300);
    }, 3000);
}

function formatCurrency(value) {
    return new Intl.NumberFormat('pt-BR', {
        style: 'currency',
        currency: 'BRL'
    }).format(value);
}

function formatDate(dateString) {
    if (!dateString) return '-';
    const date = new Date(dateString);
    return date.toLocaleDateString('pt-BR');
}