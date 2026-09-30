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
          const [res, customers, authUsers] = await Promise.all([
            apiService.getAllAccounts(),
            apiService.getAllCustomers().catch(() => []),
            apiService.getAllAuthUsers().catch(() => [])
          ]);

          const userEmailMap = {};
          (Array.isArray(authUsers) ? authUsers : []).forEach(u => {
            if (u && u.customerId && u.email) {
              userEmailMap[u.customerId] = u.email;
            }
          });

          const customerMap = {};
          const customerKycMap = {};
          (Array.isArray(customers) ? customers : []).forEach(c => {
            if (c && c.customerId) {
              if (c.customerName && c.customerName !== c.customerId) {
                customerMap[c.customerId] = c.customerName;
              } else if (c.firstName) {
                customerMap[c.customerId] = (c.firstName + ' ' + (c.lastName || '')).trim();
              }
              customerKycMap[c.customerId] = c.kycCompleted === true;
            }
          });

          const accountsList = (Array.isArray(res) ? res : []).map(acc => {
            const isSeed = [1, 2, 21, 41].includes(Number(acc.id)) || ['C5FCC99032132', 'C0106071918AA', 'CFA32EB91C801'].includes(String(acc.customerId || '').toUpperCase());
            const kycDone = isSeed || !!customerKycMap[acc.customerId];

            const email = userEmailMap[acc.customerId] || '';
            const profileName = customerMap[acc.customerId] || '';
            let userName = '';
            if (profileName && email) {
              userName = `${profileName} (${email})`;
            } else if (profileName) {
              userName = profileName;
            } else if (email) {
              userName = email;
            } else {
              userName = 'User ' + String(acc.customerId).substring(0, 8);
            }

            return {
              ...acc,
              customerName: userName,
              accountUserName: userName,
              kycCompleted: kycDone,
              kycStatus: kycDone ? 'COMPLETED' : 'PENDING'
            };
          });

          self.allAccounts(accountsList);

          // Asynchronously resolve missing emails and KYC profile status for accounts
          const uniqueCustIds = [...new Set(accountsList.map(a => a.customerId).filter(Boolean))];

          // 1. Resolve missing emails
          const missingEmailCustIds = uniqueCustIds.filter(cid => !userEmailMap[cid]);
          if (missingEmailCustIds.length > 0) {
            Promise.all(missingEmailCustIds.map(cid => apiService.getUserByCustomerId(cid).catch(() => null)))
              .then(results => {
                let updated = false;
                results.forEach(u => {
                  if (u && u.customerId && u.email) {
                    userEmailMap[u.customerId] = u.email;
                    updated = true;
                  }
                });
                if (updated) {
                  self.allAccounts(self.allAccounts().map(acc => {
                    const email = userEmailMap[acc.customerId] || '';
                    const profileName = customerMap[acc.customerId] || '';
                    let uName = '';
                    if (profileName && email) {
                      uName = `${profileName} (${email})`;
                    } else if (profileName) {
                      uName = profileName;
                    } else if (email) {
                      uName = email;
                    } else {
                      uName = acc.accountUserName;
                    }
                    return { ...acc, customerName: uName, accountUserName: uName };
                  }));
                }
              });
          }

          // 2. Resolve KYC profile setup for any accounts not yet marked complete
          const unverifiedKycCustIds = uniqueCustIds.filter(cid => !customerKycMap[cid]);
          if (unverifiedKycCustIds.length > 0) {
            Promise.all(unverifiedKycCustIds.map(cid => apiService.getCustomerKyc(cid).catch(() => null)))
              .then(kycResults => {
                let kycUpdated = false;
                kycResults.forEach(kr => {
                  if (kr && kr.customerId && kr.kycCompleted === true) {
                    customerKycMap[kr.customerId] = true;
                    if (kr.profile && kr.profile.firstName) {
                      const pName = (kr.profile.firstName + ' ' + (kr.profile.lastName || '')).trim();
                      if (pName) customerMap[kr.customerId] = pName;
                    }
                    kycUpdated = true;
                  }
                });
                if (kycUpdated) {
                  self.allAccounts(self.allAccounts().map(acc => {
                    if (customerKycMap[acc.customerId]) {
                      const email = userEmailMap[acc.customerId] || '';
                      const profileName = customerMap[acc.customerId] || '';
                      let uName = acc.accountUserName;
                      if (profileName && email) {
                        uName = `${profileName} (${email})`;
                      } else if (profileName) {
                        uName = profileName;
                      }
                      return {
                        ...acc,
                        customerName: uName,
                        accountUserName: uName,
                        kycCompleted: true,
                        kycStatus: 'COMPLETED'
                      };
                    }
                    return acc;
                  }));
                }
              });
          }
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
        self.depositTargetCustomerId(acc.customerId || '');
        self.depositTargetKyc(acc.kycCompleted === true);
        await self.handleInspectAccount();
        scrollToSection('accountInspectorSection');
      };

      this.depositTargetKyc = ko.observable(true);
      this.depositTargetCustomerId = ko.observable('');

      this.selectAccountForDeposit = (acc) => {
        if (!acc) return;
        self.depositAccountId(String(acc.id));
        self.depositAccountNumber(acc.accountNumber || '');
        self.depositTargetCustomerId(acc.customerId || '');
        self.depositTargetKyc(acc.kycCompleted === true);
        self.targetAccountId(String(acc.id));
        if (!acc.kycCompleted && acc.customerId) {
          apiService.getCustomerKyc(acc.customerId).then(kr => {
            if (kr && kr.kycCompleted) {
              acc.kycCompleted = true;
              acc.kycStatus = 'COMPLETED';
              self.depositTargetKyc(true);
              self.allAccounts(self.allAccounts().map(a => String(a.id) === String(acc.id) ? { ...a, kycCompleted: true, kycStatus: 'COMPLETED' } : a));
            }
          }).catch(() => {});
        }
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
        if (!acc.kycCompleted && acc.customerId) {
          apiService.getCustomerKyc(acc.customerId).then(kr => {
            if (kr && kr.kycCompleted) {
              acc.kycCompleted = true;
              acc.kycStatus = 'COMPLETED';
              self.allAccounts(self.allAccounts().map(a => String(a.id) === String(acc.id) ? { ...a, kycCompleted: true, kycStatus: 'COMPLETED' } : a));
            }
          }).catch(() => {});
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
          self.depositTargetCustomerId(acc.customerId || '');
          self.depositTargetKyc(acc.kycCompleted === true);
        }
        const dialog = document.getElementById('adminDepositDialog');
        if (dialog) dialog.open();
      };

      this.depositAccountId.subscribe((val) => {
        if (!val) {
          self.depositTargetKyc(true);
          self.depositTargetCustomerId('');
          return;
        }
        const acc = self.allAccounts().find(a => String(a.id) === String(val).trim());
        if (acc) {
          self.depositTargetCustomerId(acc.customerId || '');
          self.depositTargetKyc(acc.kycCompleted === true);
        }
      });

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
        if (amount > 10000000) {
          self.errorMessage('Deposit amount exceeds maximum allowed limit of ₹1,00,00,000.00 (1 Crore INR).');
          return;
        }

        // Enforce compulsory KYC profile setup before deposit
        const targetAcc = self.allAccounts().find(a => String(a.id) === String(accId));
        if (targetAcc && targetAcc.kycCompleted === false) {
          self.errorMessage(`Deposit Blocked: Customer Profile Setup (KYC) is compulsory. Customer "${targetAcc.accountUserName || targetAcc.customerId}" has not completed their KYC profile setup yet.`);
          return;
        }
        if (self.depositTargetKyc() === false) {
          self.errorMessage('Deposit Blocked: Customer Profile Setup (KYC) is compulsory before deposits can be accepted.');
          return;
        }

        self.isSubmittingDeposit(true);
        try {
          await apiService.creditAccount(
            accId,
            amount,
            self.depositDescription() || 'Admin Direct Deposit'
          );
          self.successMessage(`Successfully deposited ₹${amount.toLocaleString('en-IN', { minimumFractionDigits: 2 })} into Account #${accId}! Confirmation credit email dispatched to customer.`);
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

        self.errorMessage('');
        self.successMessage('');

        const accId = self.targetAccountId().trim();
        const targetStatus = self.targetStatus();

        // Enforce compulsory KYC profile setup before an account can be set to ACTIVE
        let targetAcc = (self.allAccounts() || []).find(a => String(a.id) === String(accId));
        if (targetStatus === 'ACTIVE' && targetAcc && targetAcc.kycCompleted === false) {
          // Double check with live KYC service before blocking
          try {
            const liveKyc = await apiService.getCustomerKyc(targetAcc.customerId);
            if (liveKyc && liveKyc.kycCompleted) {
              targetAcc.kycCompleted = true;
              targetAcc.kycStatus = 'COMPLETED';
              self.allAccounts(self.allAccounts().map(a => String(a.id) === String(accId) ? { ...a, kycCompleted: true, kycStatus: 'COMPLETED' } : a));
            } else {
              self.errorMessage(`Cannot activate Account #${accId}: Customer KYC is incomplete. Customer "${targetAcc.accountUserName || targetAcc.customerId}" has not completed mandatory profile information setup. Profile setup must be completed before an account can be set to ACTIVE.`);
              return;
            }
          } catch (e) {
            self.errorMessage(`Cannot activate Account #${accId}: Customer KYC is incomplete. Customer "${targetAcc.accountUserName || targetAcc.customerId}" has not completed mandatory profile information setup. Profile setup must be completed before an account can be set to ACTIVE.`);
            return;
          }
        }

        self.isSubmittingStatusChange(true);

        try {
          const accId = self.targetAccountId().trim();
          const targetStatus = self.targetStatus();
          const res = await apiService.changeAccountStatus(
            accId,
            targetStatus,
            targetStatus === 'CLOSED' ? (self.closureReason() || 'Administrative compliance closure') : null
          );

          // If approved/activated, also ensure customer status in User-Service is active (Req 6)
          if (targetStatus === 'ACTIVE') {
            const acc = (self.allAccounts() || []).find(a => String(a.id) === String(accId));
            const custId = acc ? acc.customerId : (self.inspectedAccount() ? self.inspectedAccount().customerId : null);
            if (custId) {
              try {
                await apiService.updateCustomerStatus(custId, 'ACTIVE');
              } catch (e) {
                console.warn('Customer status sync warning:', e);
              }
            }
          }

          self.successMessage(`Account #${accId} status successfully transitioned to ${res.status}!`);
          await self.loadAllAccounts();
          if (self.inspectedAccount() && String(self.inspectedAccount().id) === String(accId)) {
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
          if (found) {
            acc.customerName = found.customerName;
            acc.kycCompleted = found.kycCompleted;
            acc.kycStatus = found.kycStatus;
          } else if (acc.customerId) {
            try {
              const cust = await apiService.getCustomer(acc.customerId);
              if (cust) {
                acc.customerName = cust.customerName || (cust.firstName ? (cust.firstName + ' ' + (cust.lastName || '')).trim() : cust.customerId);
                acc.kycCompleted = cust.kycCompleted === true || cust.kycStatus === 'COMPLETED' || cust.kycStatus === 'VERIFIED';
                acc.kycStatus = acc.kycCompleted ? 'COMPLETED' : 'PENDING';
              }
            } catch (ce) {
              acc.customerName = acc.customerId;
              acc.kycCompleted = false;
              acc.kycStatus = 'PENDING';
            }
          }
          self.inspectedAccount(acc);
          self.targetAccountId(String(acc.id));
          self.depositAccountId(String(acc.id));
          self.depositAccountNumber(acc.accountNumber || '');
          self.depositTargetCustomerId(acc.customerId || '');
          self.depositTargetKyc(acc.kycCompleted === true);
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
      this.auditCategoryFilter = ko.observable('TRANSACTIONS'); // Exclusively transactions (Req 7)

      this.auditCounts = ko.computed(() => {
        const all = self.auditLogs() || [];
        let tx = 0;
        for (const log of all) {
          const combined = `${String(log.eventType || '')} ${String(log.subject || '')} ${String(log.messageBody || '')}`.toUpperCase();
          const isOtp = combined.includes('OTP') || combined.includes('VERIFICATION') || combined.includes('AUTH') || combined.includes('PASSWORD') || combined.includes('LOGIN');
          const isSecurity = combined.includes('PIN') || combined.includes('SECURITY') || combined.includes('ACCESS');
          if (!isOtp && !isSecurity && (combined.includes('TRANSACTION') || combined.includes('TRANSFER') || combined.includes('CREDIT') || combined.includes('DEBIT') || combined.includes('DEPOSIT') || combined.includes('LEDGER') || combined.includes('SUCCESS') || combined.includes('FUND'))) {
            tx++;
          }
        }
        return { transactions: tx };
      });

      this.filteredAuditLogs = ko.computed(() => {
        const search = (self.auditSearchFilter() || '').trim().toLowerCase();
        let logs = self.auditLogs() || [];

        // Exclude OTP & Auth and PIN & Security, strictly keep transactions (Req 7)
        logs = logs.filter(log => {
          const combined = `${String(log.eventType || '')} ${String(log.subject || '')} ${String(log.messageBody || '')}`.toUpperCase();
          const isOtp = combined.includes('OTP') || combined.includes('VERIFICATION') || combined.includes('AUTH') || combined.includes('PASSWORD') || combined.includes('LOGIN');
          const isSecurity = combined.includes('PIN') || combined.includes('SECURITY') || combined.includes('ACCESS');
          if (isOtp || isSecurity) return false;
          return combined.includes('TRANSACTION') || combined.includes('TRANSFER') || combined.includes('CREDIT') || combined.includes('DEBIT') || combined.includes('DEPOSIT') || combined.includes('LEDGER') || combined.includes('SUCCESS') || combined.includes('FUND');
        });

        // Search Filter
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
