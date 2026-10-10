/**
 * Ledgerly Authentication Scripts
 * Handles login, registration, validation, theme persistence, and OAuth redirect errors.
 */

document.addEventListener('DOMContentLoaded', () => {
  initTheme();
  checkOAuthError();
  setupLoginForm();
  setupRegisterForm();
});

// Initialize Dark/Light Theme from localStorage
function initTheme() {
  const saved = localStorage.getItem('ledgerly_theme') || 
    (window.matchMedia('(prefers-color-scheme: dark)').matches ? 'dark' : 'light');
  document.documentElement.setAttribute('data-theme', saved);

  const themeToggle = document.getElementById('theme-toggle');
  if (themeToggle) {
    themeToggle.addEventListener('click', () => {
      const current = document.documentElement.getAttribute('data-theme');
      const next = current === 'dark' ? 'light' : 'dark';
      document.documentElement.setAttribute('data-theme', next);
      localStorage.setItem('ledgerly_theme', next);
    });
  }
}

// Check for OAuth Error in URL params
function checkOAuthError() {
  const urlParams = new URLSearchParams(window.location.search);
  const errorMsg = urlParams.get('error');
  if (errorMsg) {
    const banner = document.getElementById('auth-error-banner');
    if (banner) {
      banner.style.display = 'flex';
      banner.querySelector('.error-text').textContent = decodeURIComponent(errorMsg);
    }
  }
}

// Setup Login Form
function setupLoginForm() {
  const form = document.getElementById('login-form');
  if (!form) return;

  form.addEventListener('submit', async (e) => {
    e.preventDefault();
    clearErrors();

    const email = document.getElementById('email').value.trim();
    const password = document.getElementById('password').value;
    const btn = document.getElementById('submit-btn');

    if (!email || !password) {
      showFieldError('email', 'Email and password are required');
      return;
    }

    try {
      btn.disabled = true;
      btn.textContent = 'Verifying credentials...';

      await Api.post('/api/auth/login', { email, password });
      window.location.href = '/index.html';
    } catch (err) {
      btn.disabled = false;
      btn.textContent = 'Sign In to Ledger';
      const banner = document.getElementById('auth-error-banner');
      if (banner) {
        banner.style.display = 'flex';
        banner.querySelector('.error-text').textContent = err.message || 'Invalid email or password.';
      }
    }
  });
}

// Setup Register Form
function setupRegisterForm() {
  const form = document.getElementById('register-form');
  if (!form) return;

  form.addEventListener('submit', async (e) => {
    e.preventDefault();
    clearErrors();

    const displayName = document.getElementById('display-name').value.trim();
    const email = document.getElementById('email').value.trim();
    const password = document.getElementById('password').value;
    const confirmPassword = document.getElementById('confirm-password')?.value;
    const btn = document.getElementById('submit-btn');

    let hasError = false;

    if (!displayName || displayName.length < 2) {
      showFieldError('display-name', 'Name must be at least 2 characters.');
      hasError = true;
    }

    if (!email || !email.includes('@')) {
      showFieldError('email', 'Please provide a valid email address.');
      hasError = true;
    }

    if (!password || password.length < 8) {
      showFieldError('password', 'Password must be at least 8 characters long.');
      hasError = true;
    }

    if (confirmPassword !== undefined && password !== confirmPassword) {
      showFieldError('confirm-password', 'Passwords do not match.');
      hasError = true;
    }

    if (hasError) return;

    try {
      btn.disabled = true;
      btn.textContent = 'Establishing ledger...';

      await Api.post('/api/auth/register', { displayName, email, password });
      window.location.href = '/index.html';
    } catch (err) {
      btn.disabled = false;
      btn.textContent = 'Create Ledger Account';
      const banner = document.getElementById('auth-error-banner');
      if (banner) {
        banner.style.display = 'flex';
        banner.querySelector('.error-text').textContent = err.message || 'Registration failed.';
      }
    }
  });
}

function showFieldError(fieldId, message) {
  const field = document.getElementById(fieldId);
  if (field) {
    field.classList.add('error');
    const parent = field.closest('.form-group');
    if (parent) {
      const errEl = parent.querySelector('.field-error');
      if (errEl) errEl.textContent = message;
    }
  }
}

function clearErrors() {
  document.querySelectorAll('.form-control.error').forEach(el => el.classList.remove('error'));
  document.querySelectorAll('.field-error').forEach(el => el.textContent = '');
  const banner = document.getElementById('auth-error-banner');
  if (banner) banner.style.display = 'none';
}
