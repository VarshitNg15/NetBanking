/**
 * Transaction History & Account Statements ViewModel
 * Displays complete transaction ledger history, official compliance statements, and live double-entry ledger.
 */
define(['knockout', '../services/apiService', 'ojs/ojknockout', 'ojs/ojbutton', 'ojs/ojdialog'],
  function(ko, apiService) {
    'use strict';

    function TransactionsViewModel() {
      const self = this;

      this.isLoading = ko.observable(true);
      this.errorMessage = ko.observable('');
      this.successMessage = ko.observable('');

      this.accounts = ko.observableArray([]);
      this.transactions = ko.observableArray([]);
      this.customerStatements = ko.observableArray([]);

      // Active Sub-Tab: 'transactions' | 'ledger' | 'statements'
      this.activeTab = ko.observable('transactions');
      this.switchTab = (tab) => {
        self.errorMessage('');
        self.successMessage('');
        self.activeTab(tab);
        if (tab === 'ledger') {
          self.loadLedger();
        } else if (tab === 'statements') {
          self.loadCustomerStatements();
        }
      };

      // Filters for Transactions
      this.selectedAccountFilter = ko.observable('ALL');
      this.searchQuery = ko.observable('');

      // Statement Generation Form State
      this.stmtAccountId = ko.observable('');
      this.stmtFromDate = ko.observable('');
      this.stmtToDate = ko.observable('');
      this.stmtFormat = ko.observable('CSV');
      this.isGeneratingStatement = ko.observable(false);

      // Statement Preview Dialog State
      this.previewStatementTitle = ko.observable('');
      this.previewStatementContent = ko.observable('');
      this.previewStatementFormat = ko.observable('CSV');
      this.previewRequestId = ko.observable('');

      // Ledger View State (Req 5)
      this.selectedLedgerAccountId = ko.observable('');
      this.ledgerEntries = ko.observableArray([]);
      this.isLoadingLedger = ko.observable(false);

      // Auto-reload ledger on account selection change
      this.selectedLedgerAccountId.subscribe((newVal) => {
        if (newVal) {
          self.loadLedger();
        }
      });

      // Filtered Transactions
      this.filteredTransactions = ko.computed(() => {
        const filterAcc = self.selectedAccountFilter();
        const search = (self.searchQuery() || '').trim().toLowerCase();
        let list = self.transactions() || [];

        if (filterAcc !== 'ALL') {
          list = list.filter(t => String(t.sourceAccountId) === String(filterAcc) || String(t.destinationAccountId) === String(filterAcc));
        }

        if (search) {
          list = list.filter(t => {
            const ref = String(t.transactionReference || '').toLowerCase();
            const desc = String(t.description || '').toLowerCase();
            const status = String(t.transactionStatus || '').toLowerCase();
            const type = String(t.transactionType || '').toLowerCase();
            return ref.includes(search) || desc.includes(search) || status.includes(search) || type.includes(search);
          });
        }

        const myAccountIds = new Set((self.accounts() || []).map(a => String(a.id)));
        const accMap = {};
        (self.accounts() || []).forEach(a => { if (a && a.id) accMap[String(a.id)] = a.accountNumber; });
        (self.allKnownAccounts || []).forEach(a => { if (a && a.id) accMap[String(a.id)] = a.accountNumber; });

        return list.map(t => {
          let isCredit = false;
          if (filterAcc !== 'ALL') {
            isCredit = String(t.destinationAccountId) === String(filterAcc);
          } else {
            isCredit = myAccountIds.has(String(t.destinationAccountId)) && !myAccountIds.has(String(t.sourceAccountId));
          }
          const srcAccNum = accMap[String(t.sourceAccountId)] || (t.sourceAccountId ? ('Acc #' + t.sourceAccountId) : '-');
          const destAccNum = accMap[String(t.destinationAccountId)] || (t.destinationAccountId ? ('Acc #' + t.destinationAccountId) : '-');
          return Object.assign({}, t, {
            isCredit: isCredit,
            sourceAccountDisplay: srcAccNum,
            destinationAccountDisplay: destAccNum
          });
        });
      });

      this.loadData = async () => {
        self.isLoading(true);
        self.errorMessage('');

        const user = apiService.getUser();
        if (!user || !user.customerId) {
          self.isLoading(false);
          return;
        }

        try {
          const transactionErrors = [];
          let validAccounts = [];

          try {
            const accs = typeof apiService.getMyAccounts === 'function'
              ? await apiService.getMyAccounts()
              : await apiService.getAccountsByCustomer(user.customerId);
            validAccounts = Array.isArray(accs) ? accs : [];
          } catch (err) {
            console.warn('Could not load accounts for transaction filters:', err);
          }

          try {
            const allAccs = await apiService.getAllAccounts();
            self.allKnownAccounts = Array.isArray(allAccs) ? allAccs : [];
          } catch (err) {
            self.allKnownAccounts = [];
          }

          self.accounts(validAccounts);

          if (validAccounts.length > 0) {
            if (!self.stmtAccountId()) {
              self.stmtAccountId(String(validAccounts[0].id));
            }
            if (!self.selectedLedgerAccountId()) {
              self.selectedLedgerAccountId(String(validAccounts[0].id));
            }
            self.loadLedger();
          }

          // 2. Load transactions
          let txns = [];
          try {
            txns = await apiService.getCustomerTransactions(user.customerId);
          } catch (err) {
            transactionErrors.push(err);
          }

          if (!Array.isArray(txns) || txns.length === 0) {
            const perAccountTxns = [];
            for (const acc of validAccounts) {
              try {
                const accTx = await apiService.getAccountTransactions(acc.id);
                if (Array.isArray(accTx)) {
                  perAccountTxns.push(...accTx);
                }
              } catch (err) {
                transactionErrors.push(err);
              }
            }
            txns = perAccountTxns;
          }

          // Deduplicate by transactionReference / ID
          const seen = new Set();
          const uniqueTxns = [];

          (Array.isArray(txns) ? txns : []).forEach(t => {
            const key = t.transactionReference || (t.transactionId ? String(t.transactionId) : (t.id ? String(t.id) : JSON.stringify(t)));
            if (!seen.has(key)) {
              seen.add(key);
              uniqueTxns.push(t);
            }
          });

          uniqueTxns.sort((a, b) => new Date(b.initiatedAt || b.completedAt || b.createdAt || 0) - new Date(a.initiatedAt || a.completedAt || a.createdAt || 0));
          self.transactions(uniqueTxns);

          if (uniqueTxns.length === 0 && transactionErrors.length > 0) {
            self.errorMessage('Could not load transaction history: ' + transactionErrors[0].message);
          }

          // 3. Load statements history in background
          self.loadCustomerStatements();

        } catch (err) {
          console.warn('Error loading transactions data:', err);
          self.errorMessage('Failed to load transaction history: ' + err.message);
        } finally {
          self.isLoading(false);
        }
      };

      // -------------------------------------------------------------
      // Live Double-Entry Ledger Loader (Req 5)
      // -------------------------------------------------------------
      this.loadLedger = async () => {
        const accId = self.selectedLedgerAccountId();
        if (!accId) return;

        self.isLoadingLedger(true);
        try {
          const list = await apiService.getAccountLedger(accId);
          self.ledgerEntries(Array.isArray(list) ? list : []);
        } catch (err) {
          console.warn('Could not load ledger:', err);
          self.ledgerEntries([]);
        } finally {
          self.isLoadingLedger(false);
        }
      };

      // -------------------------------------------------------------
      // Customer Statements History
      // -------------------------------------------------------------
      this.loadCustomerStatements = async () => {
        const user = apiService.getUser();
        if (!user || !user.customerId) return;
        try {
          const stmts = await apiService.getCustomerStatements(user.customerId);
          const accMap = {};
          (self.accounts() || []).forEach(a => { if (a && a.id) accMap[String(a.id)] = a.accountNumber; });
          (self.allKnownAccounts || []).forEach(a => { if (a && a.id) accMap[String(a.id)] = a.accountNumber; });
          const mapped = (Array.isArray(stmts) ? stmts : []).map(s => {
            return Object.assign({}, s, {
              accountDisplay: accMap[String(s.accountId)] || (s.accountId ? ('Account #' + s.accountId) : '-')
            });
          });
          self.customerStatements(mapped);
        } catch (e) {
          console.warn('Could not load customer statements:', e);
        }
      };

      // -------------------------------------------------------------
      // Statement Request & Download
      // -------------------------------------------------------------
      this.handleGenerateStatement = async () => {
        self.errorMessage('');
        self.successMessage('');

        const accId = self.stmtAccountId();
        if (!accId) {
          self.errorMessage('Please select an account for statement generation.');
          return;
        }

        self.isGeneratingStatement(true);
        try {
          const from = self.stmtFromDate() ? self.stmtFromDate() + 'T00:00:00' : null;
          const to = self.stmtToDate() ? self.stmtToDate() + 'T23:59:59' : null;
          const format = self.stmtFormat() || 'CSV';

          // 1. Request statement generation
          const statementRes = await apiService.requestStatement(accId, from, to, format);
          const requestId = statementRes.requestId || statementRes.id;

          if (!requestId) {
            throw new Error('Statement generation did not return a valid Request ID.');
          }

          // 2. Download statement content
          const content = await apiService.downloadStatement(requestId);

          // 3. Trigger browser file download
          const ext = format === 'CSV' ? '.csv' : '.txt';
          const mime = format === 'CSV' ? 'text/csv;charset=utf-8;' : 'text/plain;charset=utf-8;';
          const filename = `NetBanking-Statement-Acc${accId}-${new Date().toISOString().substring(0, 10)}${ext}`;

          const blob = new Blob([content], { type: mime });
          const link = document.createElement('a');
          link.href = URL.createObjectURL(blob);
          link.setAttribute('download', filename);
          document.body.appendChild(link);
          link.click();
          document.body.removeChild(link);
          URL.revokeObjectURL(link.href);

          self.successMessage(`Account Statement #${requestId} (${format}) generated and downloaded successfully!`);
          await self.loadCustomerStatements();

        } catch (err) {
          self.errorMessage(err.message || 'Failed to generate account statement.');
        } finally {
          self.isGeneratingStatement(false);
        }
      };

      // -------------------------------------------------------------
      // Preview Statement On Screen
      // -------------------------------------------------------------
      this.handlePreviewStatement = async () => {
        self.errorMessage('');
        self.successMessage('');

        const accId = self.stmtAccountId();
        if (!accId) {
          self.errorMessage('Please select an account for statement preview.');
          return;
        }

        self.isGeneratingStatement(true);
        try {
          const from = self.stmtFromDate() ? self.stmtFromDate() + 'T00:00:00' : null;
          const to = self.stmtToDate() ? self.stmtToDate() + 'T23:59:59' : null;
          const format = self.stmtFormat() || 'CSV';

          // 1. Request statement generation
          const statementRes = await apiService.requestStatement(accId, from, to, format);
          const requestId = statementRes.requestId || statementRes.id;

          if (!requestId) {
            throw new Error('Statement generation failed.');
          }

          // 2. Download statement content for display
          const content = await apiService.downloadStatement(requestId);
          self.previewRequestId(String(requestId));
          self.previewStatementTitle(`Statement #${requestId} (Account #${accId} - ${format})`);
          self.previewStatementFormat(format);
          self.previewStatementContent(content);

          const dialog = document.getElementById('statementPreviewDialog');
          if (dialog) dialog.open();

          await self.loadCustomerStatements();
        } catch (err) {
          self.errorMessage(err.message || 'Failed to preview statement.');
        } finally {
          self.isGeneratingStatement(false);
        }
      };

      this.downloadSpecificStatement = async (statement) => {
        const reqId = statement.requestId || statement.id;
        const format = statement.requestType || 'CSV';
        try {
          const content = await apiService.downloadStatement(reqId);
          const ext = format === 'CSV' ? '.csv' : '.txt';
          const mime = format === 'CSV' ? 'text/csv;charset=utf-8;' : 'text/plain;charset=utf-8;';
          const filename = `NetBanking-Statement-Acc${statement.accountId}-${reqId}${ext}`;

          const blob = new Blob([content], { type: mime });
          const link = document.createElement('a');
          link.href = URL.createObjectURL(blob);
          link.setAttribute('download', filename);
          document.body.appendChild(link);
          link.click();
          document.body.removeChild(link);
          URL.revokeObjectURL(link.href);

          self.successMessage(`Statement #${reqId} downloaded successfully.`);
        } catch (err) {
          self.errorMessage('Failed to download statement #' + reqId + ': ' + err.message);
        }
      };

      this.viewSpecificStatement = async (statement) => {
        const reqId = statement.requestId || statement.id;
        const format = statement.requestType || 'CSV';
        try {
          const content = await apiService.downloadStatement(reqId);
          self.previewRequestId(String(reqId));
          self.previewStatementTitle(`Statement #${reqId} (Account #${statement.accountId} - ${format})`);
          self.previewStatementFormat(format);
          self.previewStatementContent(content);

          const dialog = document.getElementById('statementPreviewDialog');
          if (dialog) dialog.open();
        } catch (err) {
          self.errorMessage('Failed to load statement #' + reqId + ': ' + err.message);
        }
      };

      this.closePreviewDialog = () => {
        const dialog = document.getElementById('statementPreviewDialog');
        if (dialog) dialog.close();
      };

      this.downloadPreviewedStatement = () => {
        const reqId = self.previewRequestId();
        const format = self.previewStatementFormat() || 'TXT';
        const content = self.previewStatementContent();
        if (!content) return;
        const ext = format === 'CSV' ? '.csv' : '.txt';
        const mime = format === 'CSV' ? 'text/csv;charset=utf-8;' : 'text/plain;charset=utf-8;';
        const filename = `NetBanking-Statement-Req${reqId || 'download'}${ext}`;

        const blob = new Blob([content], { type: mime });
        const link = document.createElement('a');
        link.href = URL.createObjectURL(blob);
        link.setAttribute('download', filename);
        document.body.appendChild(link);
        link.click();
        document.body.removeChild(link);
        URL.revokeObjectURL(link.href);
      };

      this.connected = () => {
        document.title = 'Transactions & Statements - NetBanking Redwood Portal';
        self.loadData();
      };
    }

    return TransactionsViewModel;
  }
);
