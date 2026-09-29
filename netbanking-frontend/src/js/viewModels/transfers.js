/**
 * Transfers ViewModel
 * Secure intra-bank and inter-bank transfers requiring 4-digit PIN authorization.
 */
define(['knockout', '../services/apiService', 'ojs/ojknockout', 'ojs/ojbutton', 'ojs/ojinputtext'],
  function(ko, apiService) {
    'use strict';

    function TransfersViewModel() {
      const self = this;

      this.isLoading = ko.observable(true);
      this.isSubmitting = ko.observable(false);
      this.errorMessage = ko.observable('');
      this.successMessage = ko.observable('');

      // Form Observables
      this.activeAccounts = ko.observableArray([]);
      this.sourceAccountId = ko.observable('');
      this.targetAccountNumber = ko.observable('');
      this.transferAmount = ko.observable('');
      this.transferDescription = ko.observable('Fund Transfer');
      this.securityPin = ko.observable('');

      // Transfer Receipt Observable
      this.lastReceipt = ko.observable(null);

      // Selected Account details
      this.selectedAccountBalance = ko.computed(() => {
        const id = self.sourceAccountId();
        const acc = self.activeAccounts().find(a => String(a.id) === String(id));
        return acc ? acc.formattedAvailableBalance : '₹ 0.00';
      });

      // Other active accounts of the user (for quick transfer to own account)
      this.otherAccounts = ko.computed(() => {
        const currentSrc = self.sourceAccountId();
        return (self.activeAccounts() || []).filter(a => String(a.id) !== String(currentSrc));
      });

      this.selectBeneficiaryAccount = (accNumber) => {
        self.targetAccountNumber(accNumber);
      };

      this.loadUserAccounts = async () => {
        self.isLoading(true);
        self.errorMessage('');

        const user = apiService.getUser();
        if (!user || !user.customerId) {
          self.isLoading(false);
          return;
        }

        try {
          const list = await apiService.getAccountsByCustomer(user.customerId);
          const activeOnly = (list || []).filter(a => a.status === 'ACTIVE');

          const detailed = await Promise.all(activeOnly.map(async (acc) => {
            let availableBalance = '0.00';
            try {
              const bal = await apiService.getBalance(acc.id);
              availableBalance = parseFloat(bal.availableBalance || 0).toFixed(2);
            } catch (e) {
              console.warn('Balance lookup failed', e);
            }
            return Object.assign({}, acc, {
              formattedAvailableBalance: '₹ ' + availableBalance
            });
          }));

          self.activeAccounts(detailed);
          if (detailed.length > 0 && !self.sourceAccountId()) {
            self.sourceAccountId(detailed[0].id);
          }
        } catch (err) {
          self.errorMessage('Failed to load accounts for transfer: ' + err.message);
        } finally {
          self.isLoading(false);
        }
      };

      // -----------------------------------------------------------
      // Execute Transfer
      // -----------------------------------------------------------
      this.handleTransfer = async () => {
        self.errorMessage('');
        self.successMessage('');
        self.lastReceipt(null);

        if (!self.sourceAccountId()) {
          self.errorMessage('Please select a source account.');
          return;
        }

        const amt = parseFloat(self.transferAmount());
        if (isNaN(amt) || amt <= 0) {
          self.errorMessage('Please enter a valid positive transfer amount.');
          return;
        }

        const targetAcc = (self.targetAccountNumber() || '').trim().toUpperCase();
        if (!targetAcc) {
          self.errorMessage('Please enter the beneficiary account number (starting with NB).');
          return;
        }

        if (!targetAcc.startsWith('NB')) {
          self.errorMessage('Invalid Beneficiary Account Number. NetBanking transfers require an 18-digit Account Number starting with "NB" (e.g. NB221992311862871354). Transfers using Database IDs are not permitted.');
          return;
        }

        if (!self.securityPin() || self.securityPin().length !== 4) {
          self.errorMessage('Please provide your 4-digit security PIN to authorize this transfer.');
          return;
        }

        self.isSubmitting(true);
        try {
          const receipt = await apiService.transfer(
            self.sourceAccountId(),
            targetAcc,
            amt,
            self.transferDescription() || 'Fund Transfer',
            self.securityPin()
          );

          self.lastReceipt(receipt);
          self.successMessage(`Transfer of ₹ ${amt.toFixed(2)} completed successfully!`);

          // Reset inputs
          self.transferAmount('');
          self.targetAccountNumber('');
          self.securityPin('');

          // Refresh account balances
          await self.loadUserAccounts();
        } catch (err) {
          console.error('Transfer failed', err);
          self.errorMessage(err.message || 'Transfer failed. Check balance or PIN.');
        } finally {
          self.isSubmitting(false);
        }
      };

      this.connected = () => {
        if (apiService.isAdmin()) {
          apiService.navigate('admin');
          return;
        }
        document.title = 'Fund Transfers - NetBanking Redwood Portal';
        self.loadUserAccounts();
      };
    }

    return TransfersViewModel;
  }
);
