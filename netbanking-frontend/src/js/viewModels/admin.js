/**
 * Admin Command Center ViewModel
 * Enterprise Operations Portal: Customer Onboarding KYC queue, Account Governance,
 * Ledger Inspection, Dual-Control Audit Passkeys, and Microservices Fleet Monitor.
 */
define(['knockout', '../services/apiService', 'ojs/ojknockout', 'ojs/ojdialog', 'ojs/ojbutton'],
  function (ko, apiService) {
    'use strict';

    function AdminViewModel() {
      const self = this;

      this.isLoading = ko.observable(true);
      this.isAuthorized = ko.observable(apiService.isAdmin());
      this.errorMessage = ko.observable('');
      this.successMessage = ko.observable('');

      // Operational Tabs: 'governance' | 'audit' | 'fleet' (Default: 'governance')
      this.activeTab = ko.observable('governance');
      this.switchTab = (tab) => {
        self.errorMessage('');
        self.successMessage('');
        self.activeTab(tab);
        if (tab === 'governance') {
          self.loadAllAccounts();
        } else if (tab === 'audit') {
          self.loadAuditLogs();
        }
      };

      // Current Admin Identity
      this.adminUser = ko.observable(apiService.getUser() || {});
      this.adminId = ko.computed(() => self.adminUser().customerId || 'ADM100000001');
      this.adminEmail = ko.computed(() => self.adminUser().email || 'admin@netbanking.com');
      this.lastRefreshed = ko.observable(new Date().toLocaleTimeString());

      this.refreshAll = async () => {
        self.lastRefreshed(new Date().toLocaleTimeString());
        self.errorMessage('');
        self.successMessage('');
        await Promise.all([
          self.loadAllAccounts(),
          self.loadAuditLogs()
        ]);
      };
      this.loadPendingRequests = this.refreshAll;

      // -------------------------------------------------------------
      // Tab 1: Account Governance, Live Directory & Inspector
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
          const name = String(acc.customerName || '').toLowerCase();
          const status = String(acc.status || '').toLowerCase();
          const type = String(acc.accountType || '').toLowerCase();
          return id.includes(filter) || accNum.includes(filter) || custId.includes(filter) || name.includes(filter) || status.includes(filter) || type.includes(filter);
        });
      });

      this.loadAllAccounts = async () => {
        self.isLoadingAccounts(true);
        try {
          const [res, customers] = await Promise.all([
            apiService.getAllAccounts(),
            apiService.getAllCustomers().catch(() => [])
          ]);

          const customerMap = {};
          (Array.isArray(customers) ? customers : []).forEach(c => {
            if (c && c.customerId) {
              customerMap[c.customerId] = c.customerName || (c.firstName ? (c.firstName + ' ' + (c.lastName || '')).trim() : c.customerId);
            }
          });

          const accountsList = (Array.isArray(res) ? res : []).map(acc => ({
            ...acc,
            customerName: customerMap[acc.customerId] || acc.customerId
          }));

          self.allAccounts(accountsList);
        } catch (err) {
          console.warn('Failed to load accounts list', err);
        } finally {
          self.isLoadingAccounts(false);
        }
      };

      function scrollToSection(elementId) {
        setTimeout(() => {
          const el = document.getElementById(elementId);
          if (el) {
            el.scrollIntoView({ behavior: 'smooth', block: 'center' });
            el.classList.add('nb-card-highlight');
            setTimeout(() => {
              el.classList.remove('nb-card-highlight');
            }, 1800);
          }
        }, 80);
      }

      this.selectAccountForInspect = async (acc) => {
        if (!acc) return;
        self.inspectSearchQuery(String(acc.id));
        self.targetAccountId(String(acc.id));
        self.depositAccountId(String(acc.id));
        self.depositAccountNumber(acc.accountNumber || '');
        await self.handleInspectAccount();
        scrollToSection('accountInspectorSection');
      };

      this.selectAccountForDeposit = (acc) => {
        if (!acc) return;
        self.depositAccountId(String(acc.id));
        self.depositAccountNumber(acc.accountNumber || '');
        self.targetAccountId(String(acc.id));
        scrollToSection('adminDepositSection');
        setTimeout(() => {
          const input = document.getElementById('adminDepAmount');
          if (input) input.focus();
        }, 300);
      };

      this.selectAccountForStatus = (acc) => {
        if (!acc) return;
        self.targetAccountId(String(acc.id));
        if (acc.status) {
          self.targetStatus(acc.status);
        }
        scrollToSection('statusOverrideSection');
        setTimeout(() => {
          const select = document.getElementById('targetStatusSelect');
          if (select) select.focus();
        }, 300);
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
          const found = self.allAccounts().find(a => String(a.id) === String(acc.id));
          if (found && found.customerName) {
            acc.customerName = found.customerName;
          } else if (acc.customerId) {
            try {
              const cust = await apiService.getCustomer(acc.customerId);
              if (cust) {
                acc.customerName = cust.customerName || (cust.firstName ? (cust.firstName + ' ' + (cust.lastName || '')).trim() : cust.customerId);
              }
            } catch (ce) {
              acc.customerName = acc.customerId;
            }
          }
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
      // Tab 2: System Audit Logs & Double-Entry Ledger Inspector
      // -------------------------------------------------------------
      this.auditLogs = ko.observableArray([]);
      this.isLoadingAuditLogs = ko.observable(false);
      this.auditSearchFilter = ko.observable('');
      this.auditCategoryFilter = ko.observable('ALL'); // 'ALL' | 'TRANSACTIONS' | 'OTP' | 'SECURITY'

      this.auditCounts = ko.computed(() => {
        const all = self.auditLogs() || [];
        let tx = 0, otp = 0, sec = 0;
        for (const log of all) {
          const combined = `${String(log.eventType || '')} ${String(log.subject || '')} ${String(log.messageBody || '')}`.toUpperCase();
          if (combined.includes('TRANSACTION') || combined.includes('TRANSFER') || combined.includes('CREDIT') || combined.includes('DEBIT')) {
            tx++;
          }
          if (combined.includes('OTP') || combined.includes('VERIFICATION') || combined.includes('AUTH') || combined.includes('PASSWORD') || combined.includes('LOGIN')) {
            otp++;
          }
          if (combined.includes('PIN') || combined.includes('SECURITY') || combined.includes('ACCESS')) {
            sec++;
          }
        }
        return { all: all.length, transactions: tx, otp: otp, security: sec };
      });

      this.filteredAuditLogs = ko.computed(() => {
        const cat = (self.auditCategoryFilter() || 'ALL').toUpperCase();
        const search = (self.auditSearchFilter() || '').trim().toLowerCase();
        let logs = self.auditLogs();

        // 1. Category Filter (Transactions, OTP, Security, All)
        if (cat !== 'ALL') {
          logs = logs.filter(log => {
            const combined = `${String(log.eventType || '')} ${String(log.subject || '')} ${String(log.messageBody || '')}`.toUpperCase();
            if (cat === 'TRANSACTIONS') {
              return combined.includes('TRANSACTION') || combined.includes('TRANSFER') || combined.includes('CREDIT') || combined.includes('DEBIT');
            } else if (cat === 'OTP') {
              return combined.includes('OTP') || combined.includes('VERIFICATION') || combined.includes('AUTH') || combined.includes('PASSWORD') || combined.includes('LOGIN');
            } else if (cat === 'SECURITY') {
              return combined.includes('PIN') || combined.includes('SECURITY') || combined.includes('ACCESS');
            }
            return combined.includes(cat);
          });
        }

        // 2. Keyword Search
        if (search) {
          logs = logs.filter(log => {
            const id = String(log.notificationId != null ? log.notificationId : (log.id || '')).toLowerCase();
            const custId = String(log.customerId || '').toLowerCase();
            const type = String(log.eventType || '').toLowerCase();
            const email = String(log.recipientEmail || '').toLowerCase();
            const subject = String(log.subject || '').toLowerCase();
            const body = String(log.messageBody || '').toLowerCase();
            const status = String(log.status || '').toLowerCase();
            return id.includes(search) || custId.includes(search) || type.includes(search) || email.includes(search) || subject.includes(search) || body.includes(search) || status.includes(search);
          });
        }

        return logs;
      });

      this.loadAuditLogs = async () => {
        self.isLoadingAuditLogs(true);
        try {
          const list = await apiService.getAllNotifications();
          self.auditLogs(Array.isArray(list) ? list : []);
        } catch (err) {
          console.warn('Failed to load system audit logs:', err);
        } finally {
          self.isLoadingAuditLogs(false);
        }
      };

      // Ledger Inspector State for Individual Audit Logs
      this.selectedAuditLog = ko.observable(null);
      this.logCustomerAccounts = ko.observableArray([]);
      this.selectedLogAccountId = ko.observable('');
      this.logAccountLedger = ko.observableArray([]);
      this.isLoadingLogLedger = ko.observable(false);
      this.logLedgerError = ko.observable('');

      this.inspectLogLedger = async (log) => {
        if (!log) return;
        self.selectedAuditLog(log);
        self.logCustomerAccounts([]);
        self.selectedLogAccountId('');
        self.logAccountLedger([]);
        self.logLedgerError('');

        const dialog = document.getElementById('logLedgerDialog');
        if (dialog) dialog.open();

        const custId = log.customerId;
        let accounts = [];

        // 1. Fetch customer accounts via User/Account Service
        if (custId) {
          try {
            const res = await apiService.getAccountsByCustomer(custId);
            accounts = Array.isArray(res) ? res : [];
          } catch (e) {
            console.warn('Could not fetch accounts by customerId:', custId, e);
          }
        }

        // 2. Scan log subject or body for explicit account reference (e.g. "Account #22" or "NB...")
        const textToScan = `${log.subject || ''} ${log.messageBody || ''}`;
        let matchedAccountId = null;

        const accMatch = textToScan.match(/(?:account\s*#?|acc\s*#?)\s*(\d+)/i);
        if (accMatch && accMatch[1]) {
          matchedAccountId = accMatch[1];
        }

        const nbMatch = textToScan.match(/\bNB\d+\b/i);
        if (nbMatch) {
          const accByNum = (self.allAccounts() || []).find(a => a.accountNumber === nbMatch[0]);
          if (accByNum) {
            matchedAccountId = String(accByNum.id);
            if (!accounts.some(a => String(a.id) === String(accByNum.id))) {
              accounts.push(accByNum);
            }
          }
        }

        self.logCustomerAccounts(accounts);

        // 3. Select target account and fetch ledger
        if (matchedAccountId) {
          self.selectedLogAccountId(String(matchedAccountId));
          await self.fetchLogLedger(matchedAccountId);
        } else if (accounts.length > 0) {
          self.selectedLogAccountId(String(accounts[0].id));
          await self.fetchLogLedger(accounts[0].id);
        } else {
          const fallbackAcc = (self.allAccounts() || []).find(a => String(a.customerId) === String(custId) || String(a.id) === String(custId));
          if (fallbackAcc) {
            self.logCustomerAccounts([fallbackAcc]);
            self.selectedLogAccountId(String(fallbackAcc.id));
            await self.fetchLogLedger(fallbackAcc.id);
          } else {
            self.logLedgerError(`No bank accounts located for Customer ID "${custId}". You can enter an Account ID to inspect.`);
          }
        }
      };

      this.fetchLogLedger = async (accountId) => {
        const id = accountId || self.selectedLogAccountId();
        if (!id) return;
        self.isLoadingLogLedger(true);
        self.logLedgerError('');
        try {
          const list = await apiService.getAccountLedger(id);
          self.logAccountLedger(Array.isArray(list) ? list : []);
        } catch (err) {
          console.warn('Failed to load ledger for account', id, err);
          self.logLedgerError(err.message || `Failed to retrieve ledger entries for Account #${id}.`);
          self.logAccountLedger([]);
        } finally {
          self.isLoadingLogLedger(false);
        }
      };

      this.closeLogLedgerDialog = () => {
        const dialog = document.getElementById('logLedgerDialog');
        if (dialog) dialog.close();
      };

      this.jumpToAccountInspector = (accountId) => {
        self.closeLogLedgerDialog();
        self.switchTab('governance');
        self.inspectSearchQuery(String(accountId));
        self.handleInspectAccount();
        scrollToSection('accountInspectorSection');
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
        document.title = 'NetBanking';
        self.isAuthorized(apiService.isAdmin());
        if (self.isAuthorized()) {
          self.loadAllAccounts();
          self.loadAuditLogs();
        }
      };
    }

    return AdminViewModel;
  }
);
