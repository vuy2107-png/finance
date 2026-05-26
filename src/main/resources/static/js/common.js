/**
 * Finance Pro - Common JavaScript Utilities
 */

// Intercept all fetch requests globally to inject CSRF header for write requests (POST, PUT, DELETE, PATCH)
(function() {
    const originalFetch = window.fetch;
    window.fetch = function(resource, init) {
        let options = init || {};
        const method = (options.method || 'GET').toUpperCase();
        
        if (['POST', 'PUT', 'DELETE', 'PATCH'].includes(method)) {
            const tokenMeta = document.querySelector('meta[name="_csrf"]');
            const headerMeta = document.querySelector('meta[name="_csrf_header"]');
            
            if (tokenMeta && headerMeta) {
                const token = tokenMeta.content;
                const headerName = headerMeta.content;
                
                options.headers = options.headers || {};
                
                if (options.headers instanceof Headers) {
                    if (!options.headers.has(headerName)) {
                        options.headers.append(headerName, token);
                    }
                } else if (Array.isArray(options.headers)) {
                    const exists = options.headers.some(h => {
                        if (Array.isArray(h) && h.length >= 2) {
                            return h[0].toLowerCase() === headerName.toLowerCase();
                        }
                        return false;
                    });
                    if (!exists) {
                        options.headers.push([headerName, token]);
                    }
                } else {
                    if (!options.headers[headerName]) {
                        options.headers[headerName] = token;
                    }
                }
            }
        }
        return originalFetch.call(this, resource, options);
    };
})();

function openModal(id) {
    console.log("Opening modal:", id);
    const modal = document.getElementById(id);
    if (modal) {
        modal.classList.add('active');
        // Fallback for direct style if needed
        if (getComputedStyle(modal).display === 'none') {
            modal.style.display = 'flex';
        }
    } else {
        console.error("Modal not found:", id);
    }
}

function closeModal(id) {
    console.log("Closing modal:", id);
    const modal = document.getElementById(id);
    if (modal) {
        modal.classList.remove('active');
        modal.style.display = 'none';
    }
}

function openMonthlySetup() {
    openModal('monthlySetupModal');
}

function closeMonthlySetup() {
    closeModal('monthlySetupModal');
}

function toggleHeaderDropdown(event) {
    if (event) event.stopPropagation();
    const dropdown = document.getElementById('headerDropdown');
    if (dropdown) {
        dropdown.classList.toggle('active');
    }
}

// Global click handler to close modals and dropdowns when clicking on overlay or outside
document.addEventListener('DOMContentLoaded', function() {
    window.addEventListener('click', function(event) {
        // Close modals
        if (event.target.classList.contains('modal-overlay')) {
            event.target.classList.remove('active');
            event.target.style.display = 'none';
        }

        // Close header dropdown when clicking outside
        const headerDropdown = document.getElementById('headerDropdown');
        const headerBadge = document.querySelector('.header-user-dropdown');
        if (headerDropdown && headerDropdown.classList.contains('active')) {
            if (headerBadge && !headerBadge.contains(event.target)) {
                headerDropdown.classList.remove('active');
            }
        }
    });
});

/**
 * Hiển thị thông báo Toast động
 * @param {string} message 
 * @param {string} type - 'SUCCESS', 'DANGER', 'WARNING', 'INFO'
 */
function showToast(message, type = 'SUCCESS') {
    const container = document.getElementById('toastContainer');
    if (!container) return;

    const toast = document.createElement('div');
    toast.className = `toast toast-${type.toLowerCase()}`;
    
    const icon = type === 'SUCCESS' ? 'fa-check-circle' : 
                 (type === 'DANGER' ? 'fa-exclamation-circle' : 
                 (type === 'WARNING' ? 'fa-exclamation-triangle' : 'fa-info-circle'));
    
    const iconColor = type === 'SUCCESS' ? 'var(--success)' : 
                      (type === 'DANGER' ? 'var(--danger)' : 
                      (type === 'WARNING' ? 'var(--warning)' : 'var(--primary)'));

    toast.innerHTML = `
        <i class="fas ${icon}" style="color: ${iconColor};"></i>
        <span>${message}</span>
    `;

    container.appendChild(toast);

    // Auto remove
    setTimeout(() => {
        toast.style.opacity = '0';
        toast.style.transform = 'translateX(100%)';
        toast.style.transition = 'all 0.5s ease';
        setTimeout(() => toast.remove(), 500);
    }, 4000);
}

// Gán vào window để các script khác có thể truy cập
window.showToast = showToast;
