/**
 * Ledgerly API Client & Helpers
 * Handles CSRF token extraction, standardized JSON requests, error formatting, and toasts.
 */

const Api = {
  getCsrfToken() {
    const match = document.cookie.match(/XSRF-TOKEN=([^;]+)/);
    return match ? decodeURIComponent(match[1]) : null;
  },

  async request(endpoint, options = {}) {
    const headers = {
      'Accept': 'application/json',
      ...(options.headers || {})
    };

    const csrfToken = this.getCsrfToken();
    if (csrfToken && options.method && options.method.toUpperCase() !== 'GET') {
      headers['X-XSRF-TOKEN'] = csrfToken;
    }

    if (options.body && typeof options.body === 'object' && !(options.body instanceof FormData)) {
      headers['Content-Type'] = 'application/json';
      options.body = JSON.stringify(options.body);
    }

    options.headers = headers;
    options.credentials = 'same-origin';

    try {
      const response = await fetch(endpoint, options);

      // Handle 401 Unauthorized
      if (response.status === 401) {
        if (!endpoint.includes('/api/auth/status') && !endpoint.includes('/api/auth/login')) {
          window.location.href = '/login.html';
        }
        const data = await response.json().catch(() => null);
        throw new Error(data?.message || 'Authentication session expired. Please sign in again.');
      }

      // Check if response is JSON
      const contentType = response.headers.get('content-type');
      if (contentType && contentType.includes('application/json')) {
        const json = await response.json();
        if (!response.ok || (json.success !== undefined && !json.success)) {
          const errMsg = json.message || 'Request failed';
          const errDetails = json.errors && json.errors.length > 0 ? ': ' + json.errors.join(', ') : '';
          throw new Error(errMsg + errDetails);
        }
        return json.data !== undefined ? json.data : json;
      }

      if (!response.ok) {
        throw new Error(`Server returned status ${response.status}`);
      }

      return await response.text();
    } catch (err) {
      console.error(`API Error on [${options.method || 'GET'} ${endpoint}]:`, err);
      throw err;
    }
  },

  get(endpoint) {
    return this.request(endpoint, { method: 'GET' });
  },

  post(endpoint, body) {
    return this.request(endpoint, { method: 'POST', body });
  },

  put(endpoint, body) {
    return this.request(endpoint, { method: 'PUT', body });
  },

  delete(endpoint) {
    return this.request(endpoint, { method: 'DELETE' });
  }
};

// Toast notification helper
function showToast(message, type = 'success') {
  let container = document.querySelector('.toast-container');
  if (!container) {
    container = document.createElement('div');
    container.className = 'toast-container';
    document.body.appendChild(container);
  }

  const toast = document.createElement('div');
  toast.className = `toast ${type}`;
  
  let iconSvg = type === 'error' 
    ? `<svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><circle cx="12" cy="12" r="10"/><line x1="12" y1="8" x2="12" y2="12"/><line x1="12" y1="16" x2="12.01" y2="16"/></svg>`
    : `<svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><polyline points="20 6 9 17 4 12"/></svg>`;

  toast.innerHTML = `${iconSvg}<span>${escapeHtml(message)}</span>`;
  container.appendChild(toast);

  setTimeout(() => {
    toast.style.opacity = '0';
    toast.style.transform = 'translateY(8px)';
    toast.style.transition = 'all 0.2s ease';
    setTimeout(() => toast.remove(), 200);
  }, 4000);
}

function escapeHtml(str) {
  if (!str) return '';
  const div = document.createElement('div');
  div.textContent = str;
  return div.innerHTML;
}

// Currency Formatter
function formatCurrency(amount) {
  if (amount === null || amount === undefined) return '$0.00';
  const num = typeof amount === 'number' ? amount : parseFloat(amount);
  return '$' + num.toLocaleString('en-US', { minimumFractionDigits: 2, maximumFractionDigits: 2 });
}
