/**
 * NetBanking API Client Service
 * Connects Oracle JET Frontend to the Spring Cloud API Gateway (http://localhost:8080)
 */
define([], function() {
  'use strict';

  const GATEWAY_URL = 'http://localhost:8080';

  class ApiService {
    constructor() {
      this.baseUrl = GATEWAY_URL;
      this.authListeners = [];
      this.router = null;
    }

    setRouter(router) {
      this.router = router;
    }

    navigate(path) {
      if (this.router && typeof this.router.go === 'function') {
        return this.router.go({ path: path });
      }
      window.location.search = `?ojr=${path}`;
    }

    // -------------------------------------------------------------
    // Session & Auth State Management
    // -------------------------------------------------------------
    getToken() {
      return sessionStorage.getItem('nb_access_token');
    }

    getRefreshToken() {
      return sessionStorage.getItem('nb_refresh_token');
    }

    getUser() {
      const raw = sessionStorage.getItem('nb_user');
      try {
        return raw ? JSON.parse(raw) : null;
      } catch (e) {
        return null;
      }
    }

    isAuthenticated() {
      return !!this.getToken();
    }

    isAdmin() {
      const user = this.getUser();
      return user && Array.isArray(user.roles) && user.roles.includes('ADMIN');
    }

    setSession(tokenResponse) {
      if (tokenResponse.accessToken) {
        sessionStorage.setItem('nb_access_token', tokenResponse.accessToken);
      }
      if (tokenResponse.refreshToken) {
        sessionStorage.setItem('nb_refresh_token', tokenResponse.refreshToken);
      }
      const user = {
        customerId: tokenResponse.customerId || '',
        roles: tokenResponse.roles || [],
        isAdmin: (tokenResponse.roles || []).includes('ADMIN'),
        email: tokenResponse.email || (this.getUser() ? this.getUser().email : '')
      };
      sessionStorage.setItem('nb_user', JSON.stringify(user));
      this.notifyAuthChange(user);
    }

    clearSession() {
      sessionStorage.removeItem('nb_access_token');
      sessionStorage.removeItem('nb_refresh_token');
      sessionStorage.removeItem('nb_user');
      this.notifyAuthChange(null);
      this.navigate('login');
    }

    onAuthChange(callback) {
      this.authListeners.push(callback);
    }

    notifyAuthChange(user) {
      this.authListeners.forEach(cb => {
        try { cb(user); } catch (e) { console.error('Auth listener error', e); }
      });
    }

    // -------------------------------------------------------------
    // Generic HTTP Request Helper
    // -------------------------------------------------------------
    async request(endpoint, options = {}) {
      const url = `${this.baseUrl}${endpoint}`;
      const headers = Object.assign({
        'Content-Type': 'application/json',
        'Accept': 'application/json'
      }, options.headers || {});

      const token = this.getToken();
      if (token && !headers['Authorization']) {
        headers['Authorization'] = `Bearer ${token}`;
      }

      options.headers = headers;

      try {
        const response = await fetch(url, options);

        // Handle 401 Unauthorized with token refresh if possible
        if (response.status === 401 && this.getRefreshToken() && !endpoint.includes('/auth/')) {
          const refreshed = await this.refresh();
          if (refreshed) {
            headers['Authorization'] = `Bearer ${this.getToken()}`;
            return fetch(url, options).then(r => this.handleResponse(r));
          } else {
            this.clearSession();
            throw new Error('Session expired. Please log in again.');
          }
        }

        return this.handleResponse(response);
      } catch (err) {
        // Detect connection refused or backend offline
        if (err.name === 'TypeError' || (err.message && err.message.toLowerCase().includes('fetch'))) {
          const offlineErr = new Error(
            'Cannot reach Backend API Gateway (http://localhost:8080). Please ensure your microservices and Podman containers are running.'
          );
          offlineErr.status = 0;
          console.error(`Backend Unreachable on [${options.method || 'GET'}] ${endpoint}:`, err);
          throw offlineErr;
        }
        console.error(`API Error on [${options.method || 'GET'}] ${endpoint}:`, err);
        throw err;
      }
    }

    async handleResponse(response) {
      const contentType = response.headers.get('content-type') || '';
      let data = null;
      if (contentType.includes('application/json')) {
        data = await response.json();
      } else {
        const text = await response.text();
        data = text ? { message: text } : {};
      }

      if (!response.ok) {
        const errorMsg = (data && (data.message || data.error || (data.details && Object.values(data.details).join(', ')))) 
          || `HTTP error ${response.status}: ${response.statusText}`;
        const err = new Error(errorMsg);
        err.status = response.status;
        err.data = data;
        throw err;
      }

      return data;
    }

    // -------------------------------------------------------------
    // Authentication Endpoints
    // -------------------------------------------------------------
    async login(email, password, otp = null) {
      const body = { email, password };
      if (otp) body.otp = otp;
      const res = await this.request('/api/v1/auth/login', {
        method: 'POST',
        body: JSON.stringify(body)
      });
      if (res && res.accessToken) {
        res.email = email;
        this.setSession(res);
      }
      return res;
    }

    async verifyOtp(email, otp, purpose = 'LOGIN') {
      return this.request('/api/v1/auth/verify-otp', {
        method: 'POST',
        body: JSON.stringify({ email, otp, purpose })
      });
    }

    async register(email, password) {
      return this.request('/api/v1/auth/register', {
        method: 'POST',
        body: JSON.stringify({ email, password })
      });
    }

    async forgotPassword(email) {
      return this.request('/api/v1/auth/forgot-password', {
        method: 'POST',
        body: JSON.stringify({ email })
      });
    }

    async resetPassword(token, newPassword) {
      return this.request('/api/v1/auth/reset-password', {
        method: 'POST',
        body: JSON.stringify({ token, newPassword })
      });
    }

    async refresh() {
      const refreshToken = this.getRefreshToken();
      if (!refreshToken) return false;
      try {
        const res = await this.request('/api/v1/auth/refresh', {
          method: 'POST',
          body: JSON.stringify({ refreshToken })
        });
        if (res && res.accessToken) {
          this.setSession(res);
          return true;
        }
      } catch (e) {
        console.warn('Token refresh failed', e);
      }
      return false;
    }

    async logout() {
      const refreshToken = this.getRefreshToken();
      try {
        if (refreshToken) {
          await this.request('/api/v1/auth/logout', {
            method: 'POST',
            body: JSON.stringify({ refreshToken })
          });
        }
      } catch (e) {
        console.warn('Logout server notification failed', e);
      } finally {
        this.clearSession();
      }
    }

    // -------------------------------------------------------------
    // Account & Ledger Endpoints
    // -------------------------------------------------------------
    async getAccountsByCustomer(customerId) {
      return this.request(`/api/v1/accounts?customerId=${encodeURIComponent(customerId)}`);
    }

    async getAccount(accountId) {
      return this.request(`/api/v1/accounts/${accountId}`);
    }

    async getBalance(accountId) {
      return this.request(`/api/v1/accounts/${accountId}/balance`);
    }

    async createAccount(customerId, accountType = 'SAVINGS', currencyCode = 'INR') {
      return this.request('/api/v1/accounts', {
        method: 'POST',
        body: JSON.stringify({ customerId, accountType, currencyCode })
      });
    }

    async creditAccount(accountId, amount, description = 'Direct Deposit') {
      const entryReference = 'DEP-' + Date.now() + '-' + Math.floor(Math.random() * 1000);
      return this.request(`/api/v1/accounts/${accountId}/credits`, {
        method: 'POST',
        body: JSON.stringify({
          amount: parseFloat(amount),
          entryReference: entryReference,
          description: description,
          createdBy: this.getUser() ? this.getUser().customerId : 'CUSTOMER'
        })
      });
    }

    async setPin(accountId, pin) {
      return this.request(`/api/v1/accounts/${accountId}/pin`, {
        method: 'POST',
        body: JSON.stringify({ pin: String(pin) })
      });
    }

    async changeAccountStatus(accountId, status, closureReason = null) {
      const body = { status };
      if (closureReason) body.closureReason = closureReason;
      return this.request(`/api/v1/accounts/${accountId}/status`, {
        method: 'PATCH',
        body: JSON.stringify(body)
      });
    }

    // -------------------------------------------------------------
    // Transfers & Transactions Endpoints
    // -------------------------------------------------------------
    async transfer(fromAccountId, toAccountId, amount, description = 'Fund Transfer', pin = '1234') {
      return this.request('/api/v1/transfers', {
        method: 'POST',
        body: JSON.stringify({
          fromAccountId: Number(fromAccountId),
          toAccountId: Number(toAccountId),
          amount: parseFloat(amount),
          description: description,
          pin: String(pin),
          currency: 'INR'
        })
      });
    }

    async getTransactions(accountId) {
      return this.request(`/api/v1/transactions?accountId=${accountId}`);
    }

    // -------------------------------------------------------------
    // Admin Review Endpoints
    // -------------------------------------------------------------
    async getPendingAccountOpenings() {
      return this.request('/api/v1/users/admin/account-opening/pending');
    }

    async approveAccountOpening(requestId, adminId = null, reason = 'Verified KYC and documents') {
      const user = this.getUser();
      const currentAdminId = adminId || (user ? user.customerId : 'ADM100000001');
      return this.request(`/api/v1/users/admin/account-opening/${requestId}/approve`, {
        method: 'POST',
        body: JSON.stringify({
          adminId: currentAdminId,
          reason: reason
        })
      });
    }

    async rejectAccountOpening(requestId, adminId = null, reason = 'Application rejected') {
      const user = this.getUser();
      const currentAdminId = adminId || (user ? user.customerId : 'ADM100000001');
      return this.request(`/api/v1/users/admin/account-opening/${requestId}/reject`, {
        method: 'POST',
        body: JSON.stringify({
          adminId: currentAdminId,
          reason: reason
        })
      });
    }

    // -------------------------------------------------------------
    // Audit & Notifications
    // -------------------------------------------------------------
    async requestAuditAccess(adminId, reason = 'Quarterly Compliance Review') {
      return this.request('/api/v1/audit/access/request', {
        method: 'POST',
        body: JSON.stringify({ adminId, reason })
      });
    }

    async getCustomerNotifications(customerId) {
      return this.request(`/api/v1/notifications?customerId=${encodeURIComponent(customerId)}`);
    }
  }

  return new ApiService();
});
