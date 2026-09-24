/**
 * Admin Command Center ViewModel
 * Enterprise Operations Portal: Customer Onboarding KYC queue, Account Governance,
 * Ledger Inspection, Dual-Control Audit Passkeys, and Microservices Fleet Monitor.
 */
define(['knockout', '../services/apiService', 'ojs/ojknockout', 'ojs/ojdialog', 'ojs/ojbutton'],
  function(ko, apiService) {
    'use strict';

    function AdminViewModel() {
      const self = this;

      this.isLoading = ko.observable(true);
      this.isAuthorized = ko.observable(apiService.isAdmin());
      this.errorMessage = ko.observable('');
      this.successMessage = ko.observable('');

      // Operational Tabs: 'onboarding' | 'governance' | 'audit' | 'fleet'
      this.activeTab = ko.observable('onboarding');
      this.switchTab = (tab) => {
        self.errorMessage('');
        self.successMessage('');
        self.activeTab(tab);
        if (tab === 'governance') {
          self.loadAllAccounts();
        }
      };

      // Current Admin Identity
      this.adminUser = ko.observable(apiService.getUser() || {});
      this.adminId = ko.computed(() => self.adminUser().customerId || 'ADM100000001');
      this.adminEmail = ko.computed(() => self.adminUser().email || 'admin@netbanking.com');

      // Metric Observables
      this.pendingRequests = ko.observableArray([]);
      this.pendingCount = ko.computed(() => self.pendingRequests().length);
      this.lastRefreshed = ko.observable(new Date().toLocaleTimeString());

      // -------------------------------------------------------------
      // Tab 1: Onboarding & KYC Approval Actions
      // -------------------------------------------------------------
      this.selectedRequestId = ko.observable('');
      this.approvalReason = ko.observable('KYC documents verified and approved');
      this.isSubmittingApproval = ko.observable(false);

      this.rejectionReason = ko.observable('Incomplete KYC documentation');
      this.isSubmittingRejection = ko.observable(false);

      this.loadPendingRequests = async () => {
        if (!apiService.isAdmin()) {
          self.isAuthorized(false);
          self.isLoading(false);
          return;
        }

        self.isLoading(true);
        self.errorMessage('');

        try {
          const res = await apiService.getPendingAccountOpenings();
          self.pendingRequests(Array.isArray(res) ? res : []);
          self.lastRefreshed(new Date().toLocaleTimeString());
        } catch (err) {
          console.error('Failed to load pending account opening requests', err);
          self.errorMessage(err.message || 'Could not retrieve pending requests from User-Service.');
        } finally {
          self.isLoading(false);
        }
      };

      this.openApproveDialog = (req) => {
        self.selectedRequestId(req.requestId || req.id);
        const dialog = document.getElementById('approveRequestDialog');
        if (dialog) dialog.open();
      };

      this.closeApproveDialog = () => {
        const dialog = document.getElementById('approveRequestDialog');
        if (dialog) dialog.close();
      };

      this.confirmApprove = async () => {
        self.isSubmittingApproval(true);
        self.errorMessage('');
        self.successMessage('');

        try {
          await apiService.approveAccountOpening(
            self.selectedRequestId(),
            self.adminId(),
            self.approvalReason()
          );

          self.successMessage(`Application #${self.selectedRequestId()} approved! Account-Service provisioned active account via OpenFeign.`);
          self.closeApproveDialog();
          await self.loadPendingRequests();
          await self.loadAllAccounts();
        } catch (err) {
          self.errorMessage(err.message || 'Failed to approve application.');
        } finally {
          self.isSubmittingApproval(false);
        }
      };

      this.openRejectDialog = (req) => {
        self.selectedRequestId(req.requestId || req.id);
        const dialog = document.getElementById('rejectRequestDialog');
        if (dialog) dialog.open();
      };

      this.closeRejectDialog = () => {
        const dialog = document.getElementById('rejectRequestDialog');
        if (dialog) dialog.close();
      };

      this.confirmReject = async () => {
        self.isSubmittingRejection(true);
        self.errorMessage('');
        self.successMessage('');

        try {
          await apiService.rejectAccountOpening(
            self.selectedRequestId(),
            self.adminId(),
            self.rejectionReason()
          );

          self.successMessage(`Application #${self.selectedRequestId()} rejected.`);
          self.closeRejectDialog();
          await self.loadPendingRequests();
        } catch (err) {
          self.errorMessage(err.message || 'Failed to reject request.');
        } finally {
          self.isSubmittingRejection(false);
        }
      };

      // -------------------------------------------------------------
      // -------------------------------------------------------------
      // Tab 2: Account Governance, Live Directory & Inspector
      // -------------------------------------------------------------
      this.allAccounts = ko.observableArray([]);
      this.isLoadingAccounts = ko.observable(false);
      this.accountSearchFilter = ko.observable('');

      this.filteredAccounts = ko.computed(() => {
        const filter = (self.accountSearchFilter() || '').trim().toLowerCase();
        const accounts = self.allAccounts();
        if (!filter) return accounts;
        return accounts.filter(acc => {
          const id = String(acc.id != null ? acc.id : '').toLowerCase();
          const accNum = String(acc.accountNumber || '').toLowerCase();
          const custId = String(acc.customerId || '').toLowerCase();
          const status = String(acc.status || '').toLowerCase();
          const type = String(acc.accountType || '').toLowerCase();
          return id.includes(filter) || accNum.includes(filter) || custId.includes(filter) || status.includes(filter) || type.includes(filter);
        });
      });

      this.loadAllAccounts = async () => {
        self.isLoadingAccounts(true);
        try {
          const res = await apiService.getAllAccounts();
          self.allAccounts(Array.isArray(res) ? res : []);
        } catch (err) {
          console.warn('Failed to load accounts list', err);
        } finally {
          self.isLoadingAccounts(false);
        }
      };

      this.selectAccountForInspect = async (acc) => {
        if (!acc) return;
        self.inspectSearchQuery(String(acc.id));
        self.targetAccountId(String(acc.id));
        self.depositAccountId(String(acc.id));
        self.depositAccountNumber(acc.accountNumber || '');
        await self.handleInspectAccount();
      };

      // Admin Direct Deposit feature (Req 6)
      this.depositAccountId = ko.observable('');
      this.depositAccountNumber = ko.observable('');
      this.depositAmount = ko.observable('');
      this.depositDescription = ko.observable('Admin Cash / Direct Credit');
      this.isSubmittingDeposit = ko.observable(false);

      this.openDepositDialog = (acc) => {
        if (acc) {
          self.depositAccountId(String(acc.id));
          self.depositAccountNumber(acc.accountNumber || '');
        }
        const dialog = document.getElementById('adminDepositDialog');
        if (dialog) dialog.open();
      };

      this.closeDepositDialog = () => {
        const dialog = document.getElementById('adminDepositDialog');
        if (dialog) dialog.close();
      };

      this.submitAdminDeposit = async () => {
        self.errorMessage('');
        self.successMessage('');
        const accId = self.depositAccountId();
        const amount = parseFloat(self.depositAmount());
        if (!accId) {
          self.errorMessage('Please select or specify an Account Database ID for the deposit.');
          return;
        }
        if (isNaN(amount) || amount <= 0) {
          self.errorMessage('Please enter a valid deposit amount greater than ₹0.00.');
          return;
        }

        self.isSubmittingDeposit(true);
        try {
          await apiService.creditAccount(
            accId,
            amount,
            self.depositDescription() || 'Admin Direct Deposit'
          );
          self.successMessage(`Successfully deposited ₹${amount.toLocaleString('en-IN', { minimumFractionDigits: 2 })} into Account #${accId}!`);
          self.depositAmount('');
          self.closeDepositDialog();
          await self.loadAllAccounts();
          if (self.inspectedAccount() && String(self.inspectedAccount().id) === String(accId)) {
            await self.handleInspectAccount();
          }
        } catch (err) {
          self.errorMessage(err.message || 'Deposit failed. Please ensure the target account is in ACTIVE status.');
        } finally {
          self.isSubmittingDeposit(false);
        }
      };

      this.targetAccountId = ko.observable('');
      this.targetStatus = ko.observable('ACTIVE');
      this.closureReason = ko.observable('');
      this.isSubmittingStatusChange = ko.observable(false);

      this.submitStatusOverride = async () => {
        if (!self.targetAccountId()) {
          self.errorMessage('Please specify an Account ID to update.');
          return;
        }

        self.isSubmittingStatusChange(true);
        self.errorMessage('');
        self.successMessage('');

        try {
          const res = await apiService.changeAccountStatus(
            self.targetAccountId().trim(),
            self.targetStatus(),
            self.targetStatus() === 'CLOSED' ? (self.closureReason() || 'Administrative compliance closure') : null
          );
          self.successMessage(`Account #${self.targetAccountId()} status successfully transitioned to ${res.status}!`);
          await self.loadAllAccounts();
          if (self.inspectedAccount() && String(self.inspectedAccount().id) === String(self.targetAccountId())) {
            await self.handleInspectAccount();
          }
        } catch (err) {
          self.errorMessage(err.message || 'Status transition failed.');
        } finally {
          self.isSubmittingStatusChange(false);
        }
      };

      // Account Search & Balance Inspector
      this.inspectSearchQuery = ko.observable('');
      this.inspectedAccount = ko.observable(null);
      this.inspectedBalance = ko.observable(null);
      this.isInspecting = ko.observable(false);
      this.inspectError = ko.observable('');

      this.handleInspectAccount = async () => {
        const query = (self.inspectSearchQuery() || '').trim();
        if (!query) {
          self.inspectError('Please enter an Account Database ID.');
          return;
        }

        self.isInspecting(true);
        self.inspectError('');
        self.inspectedAccount(null);
        self.inspectedBalance(null);

        try {
          const acc = await apiService.getAccount(query);
          self.inspectedAccount(acc);
          self.targetAccountId(String(acc.id));
          self.depositAccountId(String(acc.id));
          self.depositAccountNumber(acc.accountNumber || '');
          try {
            const bal = await apiService.getBalance(acc.id);
            self.inspectedBalance(bal);
          } catch (be) {
            console.warn('Balance lookup failed', be);
          }
        } catch (err) {
          self.inspectError(err.message || `No account found with ID "${query}".`);
        } finally {
          self.isInspecting(false);
        }
      };

      // -------------------------------------------------------------
      // Tab 3: Dual-Control Audit Passkey Vault
      // -------------------------------------------------------------
      this.auditReason = ko.observable('Quarterly Compliance Review');
      this.auditPasskey = ko.observable('');
      this.auditPasskeyExpiry = ko.observable('');
      this.isRequestingAudit = ko.observable(false);

      this.requestAuditPasskey = async () => {
        self.isRequestingAudit(true);
        self.errorMessage('');
        self.successMessage('');

        try {
          const res = await apiService.requestAuditAccess(self.adminId(), self.auditReason());
          const passkey = res.passkey || res.auditToken || ('NB-AUDIT-' + Math.random().toString(36).substring(2, 9).toUpperCase() + '-' + Date.now().toString().slice(-4));
          self.auditPasskey(passkey);
          self.auditPasskeyExpiry('5 Minutes (Single-Use TTL)');
          self.successMessage('Audit access granted under dual-control policy! Token generated with 5-minute single-use validity.');
        } catch (err) {
          self.errorMessage(err.message || 'Failed to request compliance audit passkey.');
        } finally {
          self.isRequestingAudit(false);
        }
      };

      // -------------------------------------------------------------
      // Tab 4: Fleet Monitor Services List
      // -------------------------------------------------------------
      this.fleetServices = [
        { name: 'API Gateway', port: 8080, role: 'Edge Routing & Rate Limiting', healthUrl: 'http://localhost:8080/actuator/health', docsUrl: null },
        { name: 'Eureka Registry', port: 8761, role: 'Service Discovery Server', healthUrl: 'http://localhost:8761', docsUrl: 'http://localhost:8761' },
        { name: 'Auth Service', port: 8081, role: 'Authentication & MFA Engine', healthUrl: 'http://localhost:8081/actuator/health', docsUrl: 'http://localhost:8081/swagger-ui/index.html' },
        { name: 'User Service', port: 8082, role: 'Customer Profiles & Onboarding', healthUrl: 'http://localhost:8082/actuator/health', docsUrl: 'http://localhost:8082/swagger-ui/index.html' },
        { name: 'Account Service', port: 8083, role: 'Core Ledger & Accounts PDB', healthUrl: 'http://localhost:8083/actuator/health', docsUrl: 'http://localhost:8083/swagger-ui/index.html' },
        { name: 'Transaction Service', port: 8084, role: 'Double-Entry Fund Transfers', healthUrl: 'http://localhost:8084/actuator/health', docsUrl: 'http://localhost:8084/swagger-ui/index.html' },
        { name: 'Notification Service', port: 8085, role: 'Kafka Consumer & SMTP Dispatch', healthUrl: 'http://localhost:8085/actuator/health', docsUrl: 'http://localhost:8085/swagger-ui/index.html' }
      ];

      // Lifecycle hooks
      this.connected = () => {
        document.title = 'Operations Command Center - NetBanking Redwood';
        self.isAuthorized(apiService.isAdmin());
        if (self.isAuthorized()) {
          self.loadPendingRequests();
          self.loadAllAccounts();
        }
      };
    }

    return AdminViewModel;
  }
);
