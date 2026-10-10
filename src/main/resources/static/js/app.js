/**
 * Ledgerly Application Master Controller
 * Controls Dashboard, Ledger, Budget Envelopes, Taxonomy Management, and Visual Charts.
 */

const App = {
  user: null,
  period: {
    year: new Date().getFullYear(),
    month: new Date().getMonth() + 1
  },
  categories: [],
  filters: {
    page: 0,
    size: 10,
    sortBy: 't.transaction_date',
    sortDirection: 'DESC',
    search: '',
    type: '',
    categoryId: '',
    startDate: '',
    endDate: ''
  },
  charts: {
    trends: null,
    category: null
  },

  async init() {
    this.initTheme();
    const isAuthed = await this.checkAuth();
    if (!isAuthed) return;

    this.setupNavigation();
    this.setupPeriodNavigator();
    this.setupModals();
    this.setupTransactionListeners();
    this.setupCategoryListeners();
    this.setupBudgetListeners();
    this.setupLogout();

    await this.loadCategories();
    this.handleRoute();
    window.addEventListener('hashchange', () => this.handleRoute());
  },

  initTheme() {
    const saved = localStorage.getItem('ledgerly_theme') || 
      (window.matchMedia('(prefers-color-scheme: dark)').matches ? 'dark' : 'light');
    document.documentElement.setAttribute('data-theme', saved);

    const toggle = document.getElementById('theme-toggle');
    if (toggle) {
      toggle.addEventListener('click', () => {
        const current = document.documentElement.getAttribute('data-theme');
        const next = current === 'dark' ? 'light' : 'dark';
        document.documentElement.setAttribute('data-theme', next);
        localStorage.setItem('ledgerly_theme', next);
        if (this.charts.trends) this.loadDashboard();
      });
    }

    const mobileBtn = document.getElementById('mobile-toggle');
    if (mobileBtn) {
      mobileBtn.addEventListener('click', () => {
        document.getElementById('sidebar').classList.toggle('mobile-open');
      });
    }
  },

  async checkAuth() {
    try {
      const res = await Api.get('/api/auth/status');
      if (!res.authenticated || !res.user) {
        window.location.href = '/login.html';
        return false;
      }
      this.user = res.user;
      this.renderUserSnippet();
      return true;
    } catch (e) {
      window.location.href = '/login.html';
      return false;
    }
  },

  renderUserSnippet() {
    const avatarEl = document.getElementById('current-user-avatar');
    const nameEl = document.getElementById('current-user-name');
    const emailEl = document.getElementById('current-user-email');

    nameEl.textContent = this.user.displayName;
    emailEl.textContent = this.user.email;

    if (this.user.avatarUrl) {
      avatarEl.src = this.user.avatarUrl;
    } else {
      // Initials fallback
      const initials = (this.user.displayName || 'U').substring(0, 2).toUpperCase();
      avatarEl.src = `https://ui-avatars.com/api/?name=${encodeURIComponent(initials)}&background=1B4332&color=fff&size=72`;
    }
  },

  setupLogout() {
    const logoutBtn = document.getElementById('logout-btn');
    if (logoutBtn) {
      logoutBtn.addEventListener('click', async () => {
        try {
          await Api.post('/api/auth/logout', {});
        } catch (e) {
          // ignore
        }
        window.location.href = '/login.html';
      });
    }
  },

  setupNavigation() {
    document.querySelectorAll('.sidebar-nav .nav-link').forEach(link => {
      link.addEventListener('click', () => {
        document.getElementById('sidebar').classList.remove('mobile-open');
      });
    });
  },

  handleRoute() {
    const hash = window.location.hash.replace('#', '') || 'dashboard';
    document.querySelectorAll('.content-view').forEach(v => v.style.display = 'none');
    document.querySelectorAll('.sidebar-nav .nav-link').forEach(l => l.classList.remove('active'));

    const activeLink = document.querySelector(`.sidebar-nav .nav-link[data-tab="${hash}"]`);
    if (activeLink) activeLink.classList.add('active');

    const viewEl = document.getElementById(`view-${hash}`);
    if (viewEl) {
      viewEl.style.display = 'block';
    } else {
      document.getElementById('view-dashboard').style.display = 'block';
    }

    if (hash === 'dashboard') {
      this.loadDashboard();
    } else if (hash === 'transactions') {
      this.loadTransactions();
    } else if (hash === 'budgets') {
      this.loadBudgets();
    } else if (hash === 'categories') {
      this.loadCategoriesView();
    }
  },

  // ================= PERIOD NAVIGATOR =================
  setupPeriodNavigator() {
    this.updatePeriodDisplay();

    document.getElementById('prev-period-btn').addEventListener('click', () => {
      this.period.month -= 1;
      if (this.period.month < 1) {
        this.period.month = 12;
        this.period.year -= 1;
      }
      this.updatePeriodDisplay();
      this.onPeriodChanged();
    });

    document.getElementById('next-period-btn').addEventListener('click', () => {
      this.period.month += 1;
      if (this.period.month > 12) {
        this.period.month = 1;
        this.period.year += 1;
      }
      this.updatePeriodDisplay();
      this.onPeriodChanged();
    });
  },

  updatePeriodDisplay() {
    const monthNames = [
      'January', 'February', 'March', 'April', 'May', 'June',
      'July', 'August', 'September', 'October', 'November', 'December'
    ];
    const display = document.getElementById('period-display');
    if (display) {
      display.textContent = `${monthNames[this.period.month - 1]} ${this.period.year}`;
    }
  },

  onPeriodChanged() {
    const hash = window.location.hash.replace('#', '') || 'dashboard';
    if (hash === 'dashboard') this.loadDashboard();
    if (hash === 'budgets') this.loadBudgets();
  },

  // ================= CATEGORIES =================
  async loadCategories() {
    try {
      this.categories = await Api.get('/api/categories');
      this.populateCategoryDropdowns();
    } catch (e) {
      console.error('Error fetching categories:', e);
    }
  },

  populateCategoryDropdowns() {
    const filterCat = document.getElementById('tx-filter-category');
    if (filterCat) {
      const currentVal = filterCat.value;
      filterCat.innerHTML = '<option value="">All Categories</option>';
      this.categories.forEach(c => {
        filterCat.innerHTML += `<option value="${c.id}">${escapeHtml(c.name)} (${c.type.toLowerCase()})</option>`;
      });
      filterCat.value = currentVal;
    }

    const budgetCat = document.getElementById('modal-budget-category');
    if (budgetCat) {
      budgetCat.innerHTML = '<option value="">Select expense category...</option>';
      this.categories.filter(c => c.type === 'EXPENSE').forEach(c => {
        budgetCat.innerHTML += `<option value="${c.id}">${escapeHtml(c.name)}</option>`;
      });
    }

    this.populateModalCategoryDropdown('EXPENSE');
  },

  populateModalCategoryDropdown(type) {
    const modalCat = document.getElementById('modal-tx-category');
    if (!modalCat) return;
    const current = modalCat.value;
    modalCat.innerHTML = '<option value="">Select category...</option>';
    this.categories.filter(c => c.type === type).forEach(c => {
      modalCat.innerHTML += `<option value="${c.id}">${escapeHtml(c.name)}</option>`;
    });
    modalCat.value = current;
  },

  // ================= VIEW: DASHBOARD =================
  async loadDashboard() {
    try {
      const data = await Api.get(`/api/dashboard?year=${this.period.year}&month=${this.period.month}`);
      
      // Update KPIs
      const cur = data.currentMonth || {};
      document.getElementById('dash-income').textContent = formatCurrency(cur.totalIncome || 0);
      document.getElementById('dash-expense').textContent = formatCurrency(cur.totalExpense || 0);
      document.getElementById('dash-savings').textContent = formatCurrency(cur.netSavings || 0);

      const income = parseFloat(cur.totalIncome || 0);
      const expense = parseFloat(cur.totalExpense || 0);
      const savings = parseFloat(cur.netSavings || 0);

      const savingsRate = income > 0 ? Math.round((savings / income) * 100) : 0;
      document.getElementById('dash-savings-rate').textContent = `Net margin: ${savingsRate}%`;

      document.getElementById('dash-income-count').textContent = `Income in period`;
      document.getElementById('dash-expense-count').textContent = `Outflow in period`;

      // Savings color
      const savEl = document.getElementById('dash-savings');
      savEl.className = `metric-value tabular-nums rule-double ${savings >= 0 ? 'savings-positive' : 'savings-negative'}`;

      // Budgets status
      const budgets = data.budgets || [];
      const exceeded = budgets.filter(b => b.budgetStatus === 'EXCEEDED').length;
      const warning = budgets.filter(b => b.budgetStatus === 'WARNING').length;

      document.getElementById('dash-budget-status').textContent = `${budgets.length} Envelopes`;
      const alertEl = document.getElementById('dash-budget-alert-text');
      if (exceeded > 0) {
        alertEl.textContent = `${exceeded} envelope(s) exceeded limit`;
        alertEl.style.color = 'var(--color-expense)';
      } else if (warning > 0) {
        alertEl.textContent = `${warning} envelope(s) near limit`;
        alertEl.style.color = 'var(--color-warning)';
      } else {
        alertEl.textContent = 'All allocations healthy';
        alertEl.style.color = 'var(--ink-muted)';
      }

      // Render Charts
      this.renderTrendsChart(data.sixMonthTrends || []);
      this.renderCategoryChart(data.categorySpending || []);

      // Render Recent Journal entries
      this.renderRecentTransactions(data.recentTransactions || []);
    } catch (e) {
      showToast('Failed to load dashboard: ' + e.message, 'error');
    }
  },

  renderRecentTransactions(list) {
    const tbody = document.getElementById('dash-recent-tx-tbody');
    if (!tbody) return;

    if (list.length === 0) {
      tbody.innerHTML = `<tr><td colspan="5" style="text-align:center; padding:2rem; color:var(--ink-muted);">No journal entries recorded for this period.</td></tr>`;
      return;
    }

    tbody.innerHTML = list.map(tx => `
      <tr>
        <td class="col-date">${tx.transactionDate}</td>
        <td>
          <span class="category-pill">
            <span class="cat-dot" style="background-color: ${tx.categoryColor || '#1B4332'};"></span>
            ${escapeHtml(tx.categoryName || 'General')}
          </span>
        </td>
        <td>${escapeHtml(tx.note || '—')}</td>
        <td><span class="method-tag">${tx.paymentMethod}</span></td>
        <td class="col-amount" style="color: ${tx.type === 'INCOME' ? 'var(--color-income)' : 'var(--color-expense)'}">
          ${tx.type === 'INCOME' ? '+' : '-'}${formatCurrency(tx.amount)}
        </td>
      </tr>
    `).join('');
  },

  renderTrendsChart(trends) {
    const ctx = document.getElementById('trendsChart');
    if (!ctx) return;

    if (this.charts.trends) {
      this.charts.trends.destroy();
    }

    const isDark = document.documentElement.getAttribute('data-theme') === 'dark';
    const textColor = isDark ? '#ABA79C' : '#57544C';
    const gridColor = isDark ? '#2C302B' : '#E6E1D5';

    const labels = trends.map(t => t.monthLabel);
    const incomeData = trends.map(t => parseFloat(t.income));
    const expenseData = trends.map(t => parseFloat(t.expense));

    this.charts.trends = new Chart(ctx, {
      type: 'bar',
      data: {
        labels: labels,
        datasets: [
          {
            label: 'Receipts (Income)',
            data: incomeData,
            backgroundColor: isDark ? '#3FA36F' : '#2D6A4F',
            borderRadius: 3
          },
          {
            label: 'Disbursements (Expense)',
            data: expenseData,
            backgroundColor: isDark ? '#E55353' : '#A22C29',
            borderRadius: 3
          }
        ]
      },
      options: {
        responsive: true,
        maintainAspectRatio: false,
        plugins: {
          legend: {
            position: 'top',
            labels: { color: textColor, font: { family: 'Plus Jakarta Sans', size: 12 } }
          },
          tooltip: {
            callbacks: {
              label: (ctx) => `${ctx.dataset.label}: ${formatCurrency(ctx.raw)}`
            }
          }
        },
        scales: {
          x: {
            grid: { color: gridColor },
            ticks: { color: textColor, font: { family: 'JetBrains Mono', size: 11 } }
          },
          y: {
            grid: { color: gridColor },
            ticks: {
              color: textColor,
              font: { family: 'JetBrains Mono', size: 11 },
              callback: (val) => '$' + val.toLocaleString()
            }
          }
        }
      }
    });
  },

  renderCategoryChart(cats) {
    const ctx = document.getElementById('categoryChart');
    if (!ctx) return;

    if (this.charts.category) {
      this.charts.category.destroy();
    }

    const isDark = document.documentElement.getAttribute('data-theme') === 'dark';
    const textColor = isDark ? '#ABA79C' : '#57544C';

    if (cats.length === 0) {
      this.charts.category = new Chart(ctx, {
        type: 'doughnut',
        data: {
          labels: ['No Expenses Recorded'],
          datasets: [{ data: [1], backgroundColor: [isDark ? '#2B2F2A' : '#E6E1D5'] }]
        },
        options: {
          responsive: true,
          maintainAspectRatio: false,
          plugins: { legend: { labels: { color: textColor } } }
        }
      });
      return;
    }

    this.charts.category = new Chart(ctx, {
      type: 'doughnut',
      data: {
        labels: cats.map(c => c.categoryName),
        datasets: [{
          data: cats.map(c => parseFloat(c.totalSpent)),
          backgroundColor: cats.map(c => c.categoryColor || '#2A9D8F'),
          borderWidth: 2,
          borderColor: isDark ? '#1A1C19' : '#FFFFFF'
        }]
      },
      options: {
        responsive: true,
        maintainAspectRatio: false,
        cutout: '65%',
        plugins: {
          legend: {
            position: 'right',
            labels: { color: textColor, boxWidth: 12, font: { family: 'Plus Jakarta Sans', size: 11 } }
          },
          tooltip: {
            callbacks: {
              label: (ctx) => ` ${ctx.label}: ${formatCurrency(ctx.raw)}`
            }
          }
        }
      }
    });
  },

  // ================= VIEW: TRANSACTIONS (GENERAL LEDGER) =================
  setupTransactionListeners() {
    const searchInput = document.getElementById('tx-search-input');
    let debounceTimer;
    if (searchInput) {
      searchInput.addEventListener('input', (e) => {
        clearTimeout(debounceTimer);
        debounceTimer = setTimeout(() => {
          this.filters.search = e.target.value.trim();
          this.filters.page = 0;
          this.loadTransactions();
        }, 300);
      });
    }

    const typeFilter = document.getElementById('tx-filter-type');
    if (typeFilter) {
      typeFilter.addEventListener('change', (e) => {
        this.filters.type = e.target.value;
        this.filters.page = 0;
        this.loadTransactions();
      });
    }

    const catFilter = document.getElementById('tx-filter-category');
    if (catFilter) {
      catFilter.addEventListener('change', (e) => {
        this.filters.categoryId = e.target.value;
        this.filters.page = 0;
        this.loadTransactions();
      });
    }

    const startFilter = document.getElementById('tx-filter-start');
    if (startFilter) {
      startFilter.addEventListener('change', (e) => {
        this.filters.startDate = e.target.value;
        this.filters.page = 0;
        this.loadTransactions();
      });
    }

    const endFilter = document.getElementById('tx-filter-end');
    if (endFilter) {
      endFilter.addEventListener('change', (e) => {
        this.filters.endDate = e.target.value;
        this.filters.page = 0;
        this.loadTransactions();
      });
    }

    const resetBtn = document.getElementById('tx-filter-reset-btn');
    if (resetBtn) {
      resetBtn.addEventListener('click', () => {
        this.filters = {
          page: 0,
          size: 10,
          sortBy: 't.transaction_date',
          sortDirection: 'DESC',
          search: '',
          type: '',
          categoryId: '',
          startDate: '',
          endDate: ''
        };
        if (searchInput) searchInput.value = '';
        if (typeFilter) typeFilter.value = '';
        if (catFilter) catFilter.value = '';
        if (startFilter) startFilter.value = '';
        if (endFilter) endFilter.value = '';
        this.loadTransactions();
      });
    }

    // Column sorting
    document.querySelectorAll('.ledger-table th.sortable').forEach(th => {
      th.addEventListener('click', () => {
        const col = th.getAttribute('data-sort');
        if (this.filters.sortBy === col) {
          this.filters.sortDirection = this.filters.sortDirection === 'ASC' ? 'DESC' : 'ASC';
        } else {
          this.filters.sortBy = col;
          this.filters.sortDirection = 'DESC';
        }
        this.loadTransactions();
      });
    });

    // Pagination
    document.getElementById('tx-prev-page-btn')?.addEventListener('click', () => {
      if (this.filters.page > 0) {
        this.filters.page -= 1;
        this.loadTransactions();
      }
    });

    document.getElementById('tx-next-page-btn')?.addEventListener('click', () => {
      this.filters.page += 1;
      this.loadTransactions();
    });

    // Export CSV
    document.getElementById('tx-export-csv-btn')?.addEventListener('click', () => {
      let url = `/api/export/csv?`;
      if (this.filters.startDate) url += `&startDate=${this.filters.startDate}`;
      if (this.filters.endDate) url += `&endDate=${this.filters.endDate}`;
      if (this.filters.categoryId) url += `&categoryId=${this.filters.categoryId}`;
      if (this.filters.type) url += `&type=${this.filters.type}`;
      window.location.href = url;
    });
  },

  async loadTransactions() {
    try {
      const q = new URLSearchParams();
      q.set('page', this.filters.page);
      q.set('size', this.filters.size);
      q.set('sortBy', this.filters.sortBy);
      q.set('sortDirection', this.filters.sortDirection);
      if (this.filters.search) q.set('search', this.filters.search);
      if (this.filters.type) q.set('type', this.filters.type);
      if (this.filters.categoryId) q.set('categoryId', this.filters.categoryId);
      if (this.filters.startDate) q.set('startDate', this.filters.startDate);
      if (this.filters.endDate) q.set('endDate', this.filters.endDate);

      const res = await Api.get(`/api/transactions?${q.toString()}`);
      this.renderTransactionsTable(res);
    } catch (e) {
      showToast('Error loading ledger: ' + e.message, 'error');
    }
  },

  renderTransactionsTable(pageData) {
    const tbody = document.getElementById('tx-table-tbody');
    const info = document.getElementById('tx-pagination-info');
    const pageNum = document.getElementById('tx-page-num');
    const prevBtn = document.getElementById('tx-prev-page-btn');
    const nextBtn = document.getElementById('tx-next-page-btn');

    const list = pageData.content || [];
    const total = pageData.totalElements || 0;
    const current = pageData.page || 0;
    const totalPages = pageData.totalPages || 0;

    pageNum.textContent = `Page ${current + 1} of ${Math.max(1, totalPages)}`;
    prevBtn.disabled = current === 0;
    nextBtn.disabled = pageData.last || (current + 1 >= totalPages);

    const startIdx = total === 0 ? 0 : current * this.filters.size + 1;
    const endIdx = Math.min(total, (current + 1) * this.filters.size);
    info.textContent = `Showing ${startIdx} to ${endIdx} of ${total} entries`;

    if (list.length === 0) {
      tbody.innerHTML = `
        <tr>
          <td colspan="7">
            <div class="empty-ledger">
              <svg class="empty-icon" viewBox="0 0 24 24" fill="none" stroke="currentColor">
                <rect x="3" y="4" width="18" height="16" rx="2"></rect>
                <line x1="16" y1="2" x2="16" y2="6"></line>
                <line x1="8" y1="2" x2="8" y2="6"></line>
                <line x1="3" y1="10" x2="21" y2="10"></line>
              </svg>
              <div class="empty-title">No Ledger Entries Found</div>
              <div class="empty-sub">No transactions matched your selected filters or search terms.</div>
              <button type="button" class="btn btn-primary" onclick="App.openTransactionModal()">+ Record New Entry</button>
            </div>
          </td>
        </tr>
      `;
      return;
    }

    tbody.innerHTML = list.map(tx => `
      <tr>
        <td class="col-date">${tx.transactionDate}</td>
        <td>
          <span style="font-family: var(--font-mono); font-size: 0.75rem; font-weight: 600; color: ${tx.type === 'INCOME' ? 'var(--color-income)' : 'var(--color-expense)'}">
            ${tx.type === 'INCOME' ? 'RECEIPT' : 'DISBURSAL'}
          </span>
        </td>
        <td>
          <span class="category-pill">
            <span class="cat-dot" style="background-color: ${tx.categoryColor || '#1B4332'};"></span>
            ${escapeHtml(tx.categoryName || 'General')}
          </span>
        </td>
        <td>${escapeHtml(tx.note || '—')}</td>
        <td><span class="method-tag">${tx.paymentMethod}</span></td>
        <td class="col-amount" style="color: ${tx.type === 'INCOME' ? 'var(--color-income)' : 'var(--color-expense)'}">
          ${tx.type === 'INCOME' ? '+' : '-'}${formatCurrency(tx.amount)}
        </td>
        <td class="col-actions">
          <button type="button" class="btn-icon" title="Edit Entry" onclick="App.editTransaction(${tx.id})">
            <svg width="15" height="15" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
              <path d="M11 4H4a2 2 0 0 0-2 2v14a2 2 0 0 0 2 2h14a2 2 0 0 0 2-2v-7"></path>
              <path d="M18.5 2.5a2.121 2.121 0 0 1 3 3L12 15l-4 1 1-4 9.5-9.5z"></path>
            </svg>
          </button>
          <button type="button" class="btn-icon" title="Delete Entry" onclick="App.deleteTransaction(${tx.id})">
            <svg width="15" height="15" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
              <polyline points="3 6 5 6 21 6"></polyline>
              <path d="M19 6v14a2 2 0 0 1-2 2H7a2 2 0 0 1-2-2V6m3 0V4a2 2 0 0 1 2-2h4a2 2 0 0 1 2 2v2"></path>
            </svg>
          </button>
        </td>
      </tr>
    `).join('');
  },

  // ================= VIEW: BUDGETS =================
  setupBudgetListeners() {
    document.getElementById('add-budget-btn')?.addEventListener('click', () => {
      this.openBudgetModal();
    });
  },

  async loadBudgets() {
    try {
      const list = await Api.get(`/api/budgets?year=${this.period.year}&month=${this.period.month}`);
      const grid = document.getElementById('budget-cards-grid');
      if (!grid) return;

      if (list.length === 0) {
        grid.innerHTML = `
          <div class="ledger-card" style="grid-column: 1 / -1; text-align: center; padding: 3rem;">
            <div class="empty-title">No Budget Envelopes Allocated</div>
            <p style="margin-bottom: 1.5rem;">Establish monthly expense allocations for ${document.getElementById('period-display')?.textContent || 'this period'}.</p>
            <button type="button" class="btn btn-primary" onclick="App.openBudgetModal()">+ Establish Envelope</button>
          </div>
        `;
        return;
      }

      grid.innerHTML = list.map(b => {
        const spent = parseFloat(b.actualSpent || 0);
        const limit = parseFloat(b.budgetAmount || 0);
        const percent = Math.min(100, parseFloat(b.percentageUsed || 0));
        const statusClass = b.budgetStatus.toLowerCase();

        return `
          <div class="budget-card">
            <div class="budget-header">
              <span class="category-pill">
                <span class="cat-dot" style="background-color: ${b.categoryColor || '#1B4332'};"></span>
                <strong style="color: var(--ink-primary);">${escapeHtml(b.categoryName)}</strong>
              </span>
              <span class="status-badge ${statusClass}">${b.budgetStatus}</span>
            </div>

            <div style="display: flex; justify-content: space-between; align-items: baseline;">
              <div>
                <div style="font-size: 0.75rem; color: var(--ink-muted); text-transform: uppercase;">Disbursed</div>
                <div class="amount" style="font-size: 1.35rem; font-weight: 600;">${formatCurrency(spent)}</div>
              </div>
              <div style="text-align: right;">
                <div style="font-size: 0.75rem; color: var(--ink-muted); text-transform: uppercase;">Ceiling</div>
                <div class="amount" style="font-size: 1.15rem; color: var(--ink-secondary);">${formatCurrency(limit)}</div>
              </div>
            </div>

            <div class="budget-progress-track">
              <div class="budget-progress-fill ${statusClass}" style="width: ${percent}%;"></div>
            </div>

            <div class="budget-meta">
              <span>${b.percentageUsed}% of limit</span>
              <span>
                ${b.remainingAmount >= 0 ? formatCurrency(b.remainingAmount) + ' left' : formatCurrency(Math.abs(b.remainingAmount)) + ' over'}
              </span>
            </div>

            <div style="display: flex; justify-content: flex-end; gap: 0.5rem; margin-top: 0.5rem; border-top: 1px solid var(--border-rule); padding-top: 0.75rem;">
              <button type="button" class="btn btn-secondary btn-sm" onclick="App.editBudget(${b.categoryId}, ${limit})">Adjust Limit</button>
              <button type="button" class="btn btn-secondary btn-sm" onclick="App.deleteBudget(${b.budgetId})">Remove</button>
            </div>
          </div>
        `;
      }).join('');
    } catch (e) {
      showToast('Error loading budgets: ' + e.message, 'error');
    }
  },

  // ================= VIEW: CATEGORIES =================
  setupCategoryListeners() {
    document.getElementById('add-category-btn')?.addEventListener('click', () => {
      this.openCategoryModal();
    });
  },

  loadCategoriesView() {
    const expenseList = document.getElementById('expense-categories-list');
    const incomeList = document.getElementById('income-categories-list');

    const expenses = this.categories.filter(c => c.type === 'EXPENSE');
    const incomes = this.categories.filter(c => c.type === 'INCOME');

    const renderItem = (cat) => `
      <div class="ledger-card" style="padding: 1rem 1.25rem; display: flex; align-items: center; justify-content: space-between;">
        <div style="display: flex; align-items: center; gap: 0.75rem;">
          <span class="cat-dot" style="background-color: ${cat.color}; width: 14px; height: 14px;"></span>
          <span style="font-weight: 600; color: var(--ink-primary);">${escapeHtml(cat.name)}</span>
          ${cat.default ? '<span style="font-size: 0.65rem; color: var(--ink-muted); text-transform: uppercase;">Standard</span>' : ''}
        </div>
        <div>
          ${!cat.default ? `
            <button type="button" class="btn-icon" title="Remove Category" onclick="App.deleteCategory(${cat.id})">
              <svg width="15" height="15" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                <polyline points="3 6 5 6 21 6"></polyline>
                <path d="M19 6v14a2 2 0 0 1-2 2H7a2 2 0 0 1-2-2V6m3 0V4a2 2 0 0 1 2-2h4a2 2 0 0 1 2 2v2"></path>
              </svg>
            </button>
          ` : ''}
        </div>
      </div>
    `;

    if (expenseList) expenseList.innerHTML = expenses.map(renderItem).join('');
    if (incomeList) incomeList.innerHTML = incomes.map(renderItem).join('');
  },

  // ================= MODALS & ACTIONS =================
  setupModals() {
    document.querySelectorAll('.close-modal-btn').forEach(btn => {
      btn.addEventListener('click', () => {
        document.querySelectorAll('.modal-overlay').forEach(m => m.classList.remove('open'));
      });
    });

    document.querySelectorAll('.modal-overlay').forEach(m => {
      m.addEventListener('click', (e) => {
        if (e.target === m) m.classList.remove('open');
      });
    });

    // Quick Add Entry
    document.getElementById('quick-add-tx-btn')?.addEventListener('click', () => {
      this.openTransactionModal();
    });

    // Radio switcher in TX Modal
    document.querySelectorAll('input[name="modal-tx-type"]').forEach(r => {
      r.addEventListener('change', (e) => {
        this.populateModalCategoryDropdown(e.target.value);
      });
    });

    // Transaction Form Submit
    document.getElementById('tx-form')?.addEventListener('submit', async (e) => {
      e.preventDefault();
      const id = document.getElementById('modal-tx-id').value;
      const type = document.querySelector('input[name="modal-tx-type"]:checked').value;
      const categoryId = document.getElementById('modal-tx-category').value;
      const amount = document.getElementById('modal-tx-amount').value;
      const transactionDate = document.getElementById('modal-tx-date').value;
      const paymentMethod = document.getElementById('modal-tx-method').value;
      const note = document.getElementById('modal-tx-note').value;

      try {
        const payload = {
          categoryId: parseInt(categoryId, 10),
          amount: parseFloat(amount),
          type,
          transactionDate,
          paymentMethod,
          note
        };

        if (id) {
          await Api.put(`/api/transactions/${id}`, payload);
          showToast('Ledger entry updated.');
        } else {
          await Api.post('/api/transactions', payload);
          showToast('Entry posted to ledger.');
        }

        document.getElementById('tx-modal').classList.remove('open');
        this.loadDashboard();
        this.loadTransactions();
      } catch (err) {
        showToast(err.message, 'error');
      }
    });

    // Budget Form Submit
    document.getElementById('budget-form')?.addEventListener('submit', async (e) => {
      e.preventDefault();
      const categoryId = document.getElementById('modal-budget-category').value;
      const amount = document.getElementById('modal-budget-amount').value;

      try {
        await Api.post('/api/budgets', {
          categoryId: parseInt(categoryId, 10),
          amount: parseFloat(amount),
          month: this.period.month,
          year: this.period.year
        });

        showToast('Budget envelope established.');
        document.getElementById('budget-modal').classList.remove('open');
        this.loadBudgets();
        this.loadDashboard();
      } catch (err) {
        showToast(err.message, 'error');
      }
    });

    // Category Form Submit & Swatches
    this.renderColorSwatches();
    document.getElementById('category-form')?.addEventListener('submit', async (e) => {
      e.preventDefault();
      const name = document.getElementById('modal-cat-name').value.trim();
      const type = document.querySelector('input[name="modal-cat-type"]:checked').value;
      const color = document.getElementById('modal-cat-color').value.trim();

      try {
        await Api.post('/api/categories', {
          name,
          type,
          color,
          icon: 'tag'
        });

        showToast('Taxonomy classification added.');
        document.getElementById('category-modal').classList.remove('open');
        await this.loadCategories();
        this.loadCategoriesView();
      } catch (err) {
        showToast(err.message, 'error');
      }
    });
  },

  renderColorSwatches() {
    const swatches = ['#1B4332', '#2A9D8F', '#E76F51', '#F4A261', '#E63946', '#A23B72', '#457B9D', '#6B705C', '#2D6A4F', '#1C1B18'];
    const container = document.getElementById('color-swatches');
    if (!container) return;

    container.innerHTML = swatches.map(hex => `
      <div style="width:24px; height:24px; border-radius:4px; background-color:${hex}; cursor:pointer; border:1px solid rgba(0,0,0,0.2);"
           onclick="document.getElementById('modal-cat-color').value='${hex}'"></div>
    `).join('');
  },

  openTransactionModal() {
    document.getElementById('tx-modal-title').textContent = 'Record Ledger Entry';
    document.getElementById('modal-tx-id').value = '';
    document.getElementById('modal-tx-amount').value = '';
    document.getElementById('modal-tx-date').value = new Date().toISOString().split('T')[0];
    document.getElementById('modal-tx-note').value = '';
    document.querySelector('input[name="modal-tx-type"][value="EXPENSE"]').checked = true;
    this.populateModalCategoryDropdown('EXPENSE');
    document.getElementById('tx-modal').classList.add('open');
  },

  async editTransaction(id) {
    try {
      const tx = await Api.get(`/api/transactions/${id}`);
      document.getElementById('tx-modal-title').textContent = 'Amend Ledger Entry';
      document.getElementById('modal-tx-id').value = tx.id;
      document.getElementById('modal-tx-amount').value = tx.amount;
      document.getElementById('modal-tx-date').value = tx.transactionDate;
      document.getElementById('modal-tx-method').value = tx.paymentMethod;
      document.getElementById('modal-tx-note').value = tx.note || '';

      const radio = document.querySelector(`input[name="modal-tx-type"][value="${tx.type}"]`);
      if (radio) radio.checked = true;
      this.populateModalCategoryDropdown(tx.type);
      document.getElementById('modal-tx-category').value = tx.categoryId;

      document.getElementById('tx-modal').classList.add('open');
    } catch (e) {
      showToast('Error opening transaction: ' + e.message, 'error');
    }
  },

  async deleteTransaction(id) {
    if (!confirm('Are you certain you wish to purge this transaction from the journal?')) return;
    try {
      await Api.delete(`/api/transactions/${id}`);
      showToast('Entry purged from ledger.');
      this.loadTransactions();
      this.loadDashboard();
    } catch (e) {
      showToast('Error: ' + e.message, 'error');
    }
  },

  openBudgetModal() {
    document.getElementById('modal-budget-id').value = '';
    document.getElementById('modal-budget-amount').value = '';
    document.getElementById('budget-modal').classList.add('open');
  },

  editBudget(categoryId, currentAmount) {
    document.getElementById('modal-budget-category').value = categoryId;
    document.getElementById('modal-budget-amount').value = currentAmount;
    document.getElementById('budget-modal').classList.add('open');
  },

  async deleteBudget(id) {
    if (!confirm('Remove this monthly budget envelope?')) return;
    try {
      await Api.delete(`/api/budgets/${id}`);
      showToast('Envelope removed.');
      this.loadBudgets();
      this.loadDashboard();
    } catch (e) {
      showToast('Error: ' + e.message, 'error');
    }
  },

  openCategoryModal() {
    document.getElementById('modal-category-id').value = '';
    document.getElementById('modal-cat-name').value = '';
    document.getElementById('category-modal').classList.add('open');
  },

  async deleteCategory(id) {
    if (!confirm('Delete this taxonomy classification?')) return;
    try {
      await Api.delete(`/api/categories/${id}`);
      showToast('Classification removed.');
      await this.loadCategories();
      this.loadCategoriesView();
    } catch (e) {
      showToast(e.message, 'error');
    }
  }
};

document.addEventListener('DOMContentLoaded', () => {
  App.init();
});
