/**
 * Accounts ViewModel
 * Full account management: balance inquiries, 4-digit PIN setup, credit postings, and account provisioning.
 */
define(['knockout', '../services/apiService', 'ojs/ojarraydataprovider', 'ojs/ojknockout', 'ojs/ojdialog', 'ojs/ojbutton'],
  function(ko, apiService, ArrayDataProvider) {
    'use strict';

    function AccountsViewModel() {
      const self = this;

      this.isLoading = ko.observable(true);
      this.errorMessage = ko.observable('');
      this.successMessage = ko.observable('');
      this.accounts = ko.observableArray([]);
      this.filterType = ko.observable('ALL');

      this.filteredAccounts = ko.computed(() => {
        const filter = self.filterType();
        if (filter === 'ALL') return self.accounts();
        return self.accounts().filter(a => a.accountType === filter || a.status === filter);
      });

      // Pin Setup Dialog observables
      this.selectedAccountId = ko.observable('');
      this.selectedAccountNumber = ko.observable('');
      this.pinCode = ko.observable('');
      this.confirmPinCode = ko.observable('');
      this.isSubmittingPin = ko.observable(false);

      // Account Limits (Req 2: Max 1 Savings and 1 Current account per user)
      this.hasSavingsAccount = ko.computed(() => {
        return self.accounts().some(a => a.accountType === 'SAVINGS' && a.status !== 'CLOSED');
      });
      this.hasCurrentAccount = ko.computed(() => {
        return self.accounts().some(a => a.accountType === 'CURRENT' && a.status !== 'CLOSED');
      });
      this.canOpenAccount = ko.computed(() => {
        return !self.hasSavingsAccount() || !self.hasCurrentAccount();
      });

      // Open Account Dialog
      this.newAccountType = ko.observable('SAVINGS');
      this.isSubmittingNewAccount = ko.observable(false);

      this.loadAccounts = async () => {
        self.isLoading(true);
        self.errorMessage('');

        const user = apiService.getUser();
        if (!user || !user.customerId) {
          self.isLoading(false);
          return;
        }

        try {
          const list = await apiService.getAccountsByCustomer(user.customerId);
          const detailed = await Promise.all((list || []).map(async (acc) => {
            let availableBalance = '0.00';
            let currentBalance = '0.00';
            if (acc.status === 'ACTIVE') {
              try {
                const bal = await apiService.getBalance(acc.id);
                availableBalance = parseFloat(bal.availableBalance || 0).toFixed(2);
                currentBalance = parseFloat(bal.currentBalance || 0).toFixed(2);
              } catch (e) {
                console.warn('Could not fetch balance for account', acc.id);
              }
            }
            return Object.assign({}, acc, {
              formattedAvailableBalance: '₹ ' + availableBalance,
              formattedCurrentBalance: '₹ ' + currentBalance
            });
          }));
          self.accounts(detailed);
        } catch (err) {
          self.errorMessage('Failed to load accounts: ' + err.message);
        } finally {
          self.isLoading(false);
        }
      };

      // -----------------------------------------------------------
      // 4-Digit PIN Dialog
      // -----------------------------------------------------------
      this.openPinDialog = (account) => {
        self.selectedAccountId(account.id);
        self.selectedAccountNumber(account.accountNumber);
        self.pinCode('');
        self.confirmPinCode('');
        const dialog = document.getElementById('setPinDialog');
        if (dialog) dialog.open();
      };

      this.closePinDialog = () => {
        const dialog = document.getElementById('setPinDialog');
        if (dialog) dialog.close();
      };

      this.submitPin = async () => {
        if (!self.pinCode() || self.pinCode().length !== 4 || isNaN(self.pinCode())) {
          self.errorMessage('PIN must be exactly 4 numeric digits.');
          return;
        }
        if (self.pinCode() !== self.confirmPinCode()) {
          self.errorMessage('PIN and confirmation PIN do not match.');
          return;
        }

        self.isSubmittingPin(true);
        try {
          await apiService.setPin(self.selectedAccountId(), self.pinCode());
          self.successMessage(`4-digit security PIN set successfully for account ${self.selectedAccountNumber()}!`);
          self.closePinDialog();
        } catch (err) {
          self.errorMessage(err.message || 'Failed to set PIN.');
        } finally {
          self.isSubmittingPin(false);
        }
      };

      // -----------------------------------------------------------
      // Create Account Dialog (1 Savings & 1 Current limit enforced)
      // -----------------------------------------------------------
      this.openCreateAccountDialog = () => {
        if (!self.canOpenAccount()) {
          self.errorMessage('Account limit reached: Platform policy permits a maximum of 1 Savings and 1 Current account per customer.');
          return;
        }
        if (self.hasSavingsAccount()) {
          self.newAccountType('CURRENT');
        } else {
          self.newAccountType('SAVINGS');
        }
        const dialog = document.getElementById('accCreateDialog');
        if (dialog) dialog.open();
      };

      this.closeCreateAccountDialog = () => {
        const dialog = document.getElementById('accCreateDialog');
        if (dialog) dialog.close();
      };

      this.submitCreateAccount = async () => {
        if (self.newAccountType() === 'SAVINGS' && self.hasSavingsAccount()) {
          self.errorMessage('You already hold an active or pending Savings Account. Only 1 Savings Account is allowed.');
          return;
        }
        if (self.newAccountType() === 'CURRENT' && self.hasCurrentAccount()) {
          self.errorMessage('You already hold an active or pending Current Account. Only 1 Current Account is allowed.');
          return;
        }

        const user = apiService.getUser();
        self.isSubmittingNewAccount(true);
        self.errorMessage('');
        self.successMessage('');

        try {
          const res = await apiService.createAccount(user.customerId, self.newAccountType(), 'INR');
          self.successMessage(`Account application for ${self.newAccountType()} submitted! Status: ${res.status}. Account Number: ${res.accountNumber}`);
          self.closeCreateAccountDialog();
          await self.loadAccounts();
        } catch (err) {
          self.errorMessage(err.message || 'Failed to create account.');
        } finally {
          self.isSubmittingNewAccount(false);
        }
      };

      this.connected = () => {
        if (apiService.isAdmin()) {
          apiService.navigate('admin');
          return;
        }
        document.title = 'Accounts - NetBanking Redwood Portal';
        self.loadAccounts();
      };
    }

    return AccountsViewModel;
  }
);
