/**
 * Notifications ViewModel
 * Displays real-time email dispatch records, security alerts, and system audit logs.
 */
define(['knockout', '../services/apiService', 'ojs/ojknockout', 'ojs/ojbutton', 'ojs/ojdialog'],
  function(ko, apiService) {
    'use strict';

    function NotificationsViewModel() {
      const self = this;

      this.isLoading = ko.observable(true);
      this.errorMessage = ko.observable('');
      this.notifications = ko.observableArray([]);

      this.user = ko.observable(apiService.getUser() || {});
      this.isAdmin = ko.observable(apiService.isAdmin());
      this.customerId = ko.computed(() => (self.user() ? self.user().customerId : '') || '');

      // Filters for Audit Logs & Notifications
      this.categoryFilter = ko.observable('ALL');
      this.searchFilter = ko.observable('');

      this.categoryCounts = ko.computed(() => {
        const all = self.notifications() || [];
        let tx = 0, otp = 0, sec = 0;
        for (const n of all) {
          const combined = `${String(n.eventType || '')} ${String(n.subject || '')} ${String(n.messageBody || '')}`.toUpperCase();
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

      this.filteredNotifications = ko.computed(() => {
        const cat = (self.categoryFilter() || 'ALL').toUpperCase();
        const search = (self.searchFilter() || '').trim().toLowerCase();
        let items = self.notifications();

        if (cat !== 'ALL') {
          items = items.filter(n => {
            const combined = `${String(n.eventType || '')} ${String(n.subject || '')} ${String(n.messageBody || '')}`.toUpperCase();
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

        if (search) {
          items = items.filter(n => {
            const id = String(n.notificationId != null ? n.notificationId : (n.id || '')).toLowerCase();
            const custId = String(n.customerId || '').toLowerCase();
            const type = String(n.eventType || '').toLowerCase();
            const email = String(n.recipientEmail || '').toLowerCase();
            const subj = String(n.subject || '').toLowerCase();
            const body = String(n.messageBody || '').toLowerCase();
            const status = String(n.status || '').toLowerCase();
            return id.includes(search) || custId.includes(search) || type.includes(search) || email.includes(search) || subj.includes(search) || body.includes(search) || status.includes(search);
          });
        }

        return items;
      });

      this.loadNotifications = async () => {
        self.isLoading(true);
        self.errorMessage('');

        const user = apiService.getUser();
        const isAdmin = apiService.isAdmin();

        try {
          let list = [];
          if (isAdmin) {
            list = await apiService.getAllNotifications();
          } else if (user && user.customerId) {
            list = await apiService.getCustomerNotifications(user.customerId);
          }

          const items = (Array.isArray(list) ? list : []).map(n => ({
            id: n.notificationId || n.id,
            notificationId: n.notificationId || n.id,
            customerId: n.customerId || (user ? user.customerId : ''),
            eventType: n.eventType || n.notificationType || 'SECURITY_ALERT',
            recipientEmail: n.recipientEmail || n.recipient || (user ? user.email : ''),
            subject: n.subject || 'Account Security Alert',
            messageBody: n.messageBody || n.content || n.body || '',
            status: n.status || 'DELIVERED',
            createdAt: n.createdAt || new Date().toISOString()
          }));

          self.notifications(items);
        } catch (err) {
          console.warn('Could not load notifications', err);
          self.errorMessage('Unable to load live notifications from Notification-Service.');
          self.notifications([]);
        } finally {
          self.isLoading(false);
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

        if (custId) {
          try {
            const res = await apiService.getAccountsByCustomer(custId);
            accounts = Array.isArray(res) ? res : [];
          } catch (e) {
            console.warn('Could not fetch accounts by customerId:', custId, e);
          }
        }

        const textToScan = `${log.subject || ''} ${log.messageBody || ''}`;
        let matchedAccountId = null;

        const accMatch = textToScan.match(/(?:account\s*#?|acc\s*#?)\s*(\d+)/i);
        if (accMatch && accMatch[1]) {
          matchedAccountId = accMatch[1];
        }

        self.logCustomerAccounts(accounts);

        if (matchedAccountId) {
          self.selectedLogAccountId(String(matchedAccountId));
          await self.fetchLogLedger(matchedAccountId);
        } else if (accounts.length > 0) {
          self.selectedLogAccountId(String(accounts[0].id));
          await self.fetchLogLedger(accounts[0].id);
        } else {
          self.logLedgerError(`No bank accounts found for Customer "${custId}". Enter an Account ID to inspect.`);
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

      this.connected = () => {
        document.title = (apiService.isAdmin() ? 'System Audit Logs' : 'Notification Center') + ' - NetBanking Redwood Portal';
        self.isAdmin(apiService.isAdmin());
        self.user(apiService.getUser() || {});
        self.loadNotifications();
      };
    }

    return NotificationsViewModel;
  }
);
