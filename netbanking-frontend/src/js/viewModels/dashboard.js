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
          self.loadNotifications();
        } catch (err) {
          console.error('Failed to load dashboard accounts', err);
          self.errorMessage('Could not load accounts. Ensure microservices are running on port 8080.');
        } finally {
          self.isLoading(false);
        }
      };

      // -----------------------------------------------------------
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

      // Customer Profile & KYC State (Req 4)
      this.customerProfile = ko.observable(null);
      this.hasProfile = ko.observable(false);
      this.isLoadingProfile = ko.observable(false);
      this.isSavingProfile = ko.observable(false);

      this.profileFirstName = ko.observable('');
      this.profileLastName = ko.observable('');
      this.profilePhoneNumber = ko.observable('');
      this.profileDateOfBirth = ko.observable('');
      this.profileAddressLine1 = ko.observable('');
      this.profileAddressLine2 = ko.observable('');
      this.profileCity = ko.observable('');
      this.profileState = ko.observable('');
      this.profilePostalCode = ko.observable('');
      this.profileCountry = ko.observable('India');

      this.loadCustomerProfile = async () => {
        const custId = self.customerId();
        if (!custId) return;
        self.isLoadingProfile(true);
        try {
          const prof = await apiService.getCustomerProfile(custId);
          if (prof && (prof.firstName || prof.id)) {
            self.customerProfile(prof);
            self.hasProfile(true);
            self.profileFirstName(prof.firstName || '');
            self.profileLastName(prof.lastName || '');
            self.profilePhoneNumber(prof.phoneNumber || '');
            self.profileDateOfBirth(prof.dateOfBirth || '');
            self.profileAddressLine1(prof.addressLine1 || '');
            self.profileAddressLine2(prof.addressLine2 || '');
            self.profileCity(prof.city || '');
            self.profileState(prof.state || '');
            self.profilePostalCode(prof.postalCode || '');
            self.profileCountry(prof.country || 'India');
          } else {
            self.hasProfile(false);
          }
        } catch (err) {
          self.hasProfile(false);
        } finally {
          self.isLoadingProfile(false);
        }
      };

      this.openProfileDialog = () => {
        const dialog = document.getElementById('customerProfileDialog');
        if (dialog) dialog.open();
      };

      this.closeProfileDialog = () => {
        const dialog = document.getElementById('customerProfileDialog');
        if (dialog) dialog.close();
      };

      this.saveProfile = async () => {
        if (!self.profileFirstName() || !self.profileFirstName().trim()) {
          self.errorMessage('First name is required to complete profile.');
          return;
        }

        self.isSavingProfile(true);
        self.errorMessage('');
        self.successMessage('');

        const payload = {
          firstName: self.profileFirstName().trim(),
          lastName: (self.profileLastName() || '').trim(),
          phoneNumber: (self.profilePhoneNumber() || '').trim(),
          dateOfBirth: self.profileDateOfBirth() || null,
          addressLine1: (self.profileAddressLine1() || '').trim(),
          addressLine2: (self.profileAddressLine2() || '').trim(),
          city: (self.profileCity() || '').trim(),
          state: (self.profileState() || '').trim(),
          postalCode: (self.profilePostalCode() || '').trim(),
          country: (self.profileCountry() || 'India').trim()
        };

        try {
          const res = await apiService.saveCustomerProfile(self.customerId(), payload);
          self.customerProfile(res);
          self.hasProfile(true);
          self.successMessage('Customer Profile details saved successfully!');
          self.closeProfileDialog();
        } catch (err) {
          self.errorMessage(err.message || 'Failed to save customer profile.');
        } finally {
          self.isSavingProfile(false);
        }
      };

      // -----------------------------------------------------------
      // Create Account Request (1 Savings & 1 Current limit enforced)
      // -----------------------------------------------------------
      this.openNewAccountDialog = () => {
        if (!self.canOpenAccount()) {
          self.errorMessage('Account limit reached: Platform policy permits a maximum of 1 Savings and 1 Current account per customer.');
          return;
        }
        if (self.hasSavingsAccount()) {
          self.newAccountType('CURRENT');
        } else {
          self.newAccountType('SAVINGS');
        }
        const dialog = document.getElementById('newAccountDialog');
        if (dialog) dialog.open();
      };

      this.closeNewAccountDialog = () => {
        const dialog = document.getElementById('newAccountDialog');
        if (dialog) dialog.close();
      };

      this.submitNewAccount = async () => {
        if (self.newAccountType() === 'SAVINGS' && self.hasSavingsAccount()) {
          self.errorMessage('You already hold an active or pending Savings Account. Only 1 Savings Account is allowed.');
          return;
        }
        if (self.newAccountType() === 'CURRENT' && self.hasCurrentAccount()) {
          self.errorMessage('You already hold an active or pending Current Account. Only 1 Current Account is allowed.');
          return;
        }

        self.isSubmittingNewAccount(true);
        self.errorMessage('');
        self.successMessage('');

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

      this.goToTransfers = () => {
        apiService.navigate('transfers');
      };

      this.goToAccounts = () => {
        apiService.navigate('accounts');
      };

      // Notification Center in User Dashboard (Req 12)
      this.recentNotifications = ko.observableArray([]);
      this.isLoadingNotifications = ko.observable(false);

      this.loadNotifications = async () => {
        const user = apiService.getUser();
        if (!user || !user.customerId) return;

        self.isLoadingNotifications(true);
        try {
          const list = await apiService.getCustomerNotifications(user.customerId);
          const mapped = (Array.isArray(list) ? list : []).map(n => ({
            id: n.notificationId || n.id,
            eventType: n.eventType || n.notificationType || 'ALERT',
            subject: n.subject || 'Account Notification',
            messageBody: n.messageBody || n.content || n.body || '',
            status: n.status || 'DELIVERED',
            createdAt: n.createdAt || new Date().toISOString()
          }));
          self.recentNotifications(mapped.slice(0, 5));
        } catch (e) {
          console.warn('Could not load dashboard notifications:', e);
          self.recentNotifications([]);
        } finally {
          self.isLoadingNotifications(false);
        }
      };

      this.goToNotifications = () => {
        apiService.navigate('notifications');
      };

      // Lifecycle hooks
      this.connected = () => {
        if (apiService.isAdmin()) {
          apiService.navigate('admin');
          return;
        }
        document.title = 'Dashboard - NetBanking Redwood Portal';
        self.loadDashboardData();
        self.loadCustomerProfile();
        self.loadNotifications();
      };
    }

    return DashboardViewModel;
  }
);
