/**
 * Dashboard ViewModel
 * Displays Redwood Metric KPI cards, customer accounts overview, and quick banking shortcuts.
 */
define(['knockout', '../services/apiService', 'ojs/ojarraydataprovider', 'ojs/ojknockout', 'ojs/ojbutton', 'ojs/ojdialog'],
  function(ko, apiService, ArrayDataProvider) {
    'use strict';

    function DashboardViewModel() {
      const self = this;

      this.isLoading = ko.observable(true);
      this.errorMessage = ko.observable('');
      this.successMessage = ko.observable('');
      this.accounts = ko.observableArray([]);
      this.accountsDataProvider = new ArrayDataProvider(this.accounts, { keyAttributes: 'id' });

      // Current user details
      this.user = ko.observable(apiService.getUser() || {});
      this.customerId = ko.computed(() => (self.user().customerId || ''));
      this.userEmail = ko.computed(() => (self.user().email || ''));
      this.isAdmin = ko.computed(() => apiService.isAdmin());

      // Metrics
      this.totalAvailableBalance = ko.observable('₹ 0.00');
      this.totalAccountsCount = ko.observable(0);
      this.activeAccountsCount = ko.observable(0);
      this.pendingAccountsCount = ko.observable(0);

      // New Account Dialog observables
      this.newAccountType = ko.observable('SAVINGS');
      this.isSubmittingNewAccount = ko.observable(false);

      // Quick Credit Dialog observables
      this.creditAccountId = ko.observable('');
      this.creditAmount = ko.observable('');
      this.creditDescription = ko.observable('Online Deposit');
      this.isSubmittingCredit = ko.observable(false);

      // -----------------------------------------------------------
      // Fetch Accounts and Balances
      // -----------------------------------------------------------
      this.loadDashboardData = async () => {
        self.isLoading(true);
        self.errorMessage('');

        const user = apiService.getUser();
        if (!user || !user.customerId) {
          self.isLoading(false);
          return;
        }

        try {
          const accs = await apiService.getAccountsByCustomer(user.customerId);
          const accountList = Array.isArray(accs) ? accs : [];

          // Load balances for each account concurrently
          let totalBal = 0;
          let activeCnt = 0;
          let pendingCnt = 0;

          const detailedAccounts = await Promise.all(accountList.map(async (acc) => {
            let availableBalance = '0.00';
            let currentBalance = '0.00';

            if (acc.status === 'ACTIVE') {
              activeCnt++;
              try {
                const bal = await apiService.getBalance(acc.id);
                availableBalance = parseFloat(bal.availableBalance || 0).toFixed(2);
                currentBalance = parseFloat(bal.currentBalance || 0).toFixed(2);
                totalBal += parseFloat(bal.availableBalance || 0);
              } catch (e) {
                console.warn('Balance lookup failed for account', acc.id, e);
              }
            } else if (acc.status === 'PENDING_APPROVAL') {
              pendingCnt++;
            }

            return Object.assign({}, acc, {
              formattedAvailableBalance: '₹ ' + availableBalance,
              formattedCurrentBalance: '₹ ' + currentBalance
            });
          }));

          self.accounts(detailedAccounts);
          self.totalAccountsCount(accountList.length);
          self.activeAccountsCount(activeCnt);
          self.pendingAccountsCount(pendingCnt);
          self.totalAvailableBalance('₹ ' + totalBal.toLocaleString('en-IN', { minimumFractionDigits: 2, maximumFractionDigits: 2 }));
        } catch (err) {
          console.error('Failed to load dashboard accounts', err);
          self.errorMessage('Could not load accounts. Ensure microservices are running on port 8080.');
        } finally {
          self.isLoading(false);
        }
      };

      // -----------------------------------------------------------
      // Create Account Request
      // -----------------------------------------------------------
      this.openNewAccountDialog = () => {
        const dialog = document.getElementById('newAccountDialog');
        if (dialog) dialog.open();
      };

      this.closeNewAccountDialog = () => {
        const dialog = document.getElementById('newAccountDialog');
        if (dialog) dialog.close();
      };

      this.submitNewAccount = async () => {
        self.isSubmittingNewAccount(true);
        try {
          const res = await apiService.createAccount(self.customerId(), self.newAccountType(), 'INR');
          self.successMessage(`Account application for ${self.newAccountType()} submitted! Status: ${res.status}`);
          self.closeNewAccountDialog();
          await self.loadDashboardData();
        } catch (err) {
          self.errorMessage(err.message || 'Failed to submit account request.');
        } finally {
          self.isSubmittingNewAccount(false);
        }
      };

      // -----------------------------------------------------------
      // Quick Credit / Deposit Action
      // -----------------------------------------------------------
      this.openCreditDialog = (account) => {
        if (account && account.id) {
          self.creditAccountId(account.id);
        } else if (self.accounts().length > 0) {
          self.creditAccountId(self.accounts()[0].id);
        }
        self.creditAmount('');
        const dialog = document.getElementById('quickCreditDialog');
        if (dialog) dialog.open();
      };

      this.closeCreditDialog = () => {
        const dialog = document.getElementById('quickCreditDialog');
        if (dialog) dialog.close();
      };

      this.submitCredit = async () => {
        if (!self.creditAccountId() || !self.creditAmount() || parseFloat(self.creditAmount()) <= 0) {
          self.errorMessage('Please specify a valid deposit amount.');
          return;
        }

        self.isSubmittingCredit(true);
        try {
          await apiService.creditAccount(self.creditAccountId(), self.creditAmount(), self.creditDescription());
          self.successMessage(`Successfully credited ₹ ${parseFloat(self.creditAmount()).toFixed(2)} to account ID ${self.creditAccountId()}!`);
          self.closeCreditDialog();
          await self.loadDashboardData();
        } catch (err) {
          self.errorMessage(err.message || 'Credit operation failed.');
        } finally {
          self.isSubmittingCredit(false);
        }
      };

      this.goToTransfers = () => {
        apiService.navigate('transfers');
      };

      this.goToAccounts = () => {
        apiService.navigate('accounts');
      };

      // Lifecycle hooks
      this.connected = () => {
        if (apiService.isAdmin()) {
          apiService.navigate('admin');
          return;
        }
        document.title = 'Dashboard - NetBanking Redwood Portal';
        self.loadDashboardData();
      };
    }

    return DashboardViewModel;
  }
);
