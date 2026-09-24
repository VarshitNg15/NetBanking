/**
 * Login & Authentication ViewModel
 * Supports Sign In, MFA OTP verification, Registration, and Forgot Password flows.
 */
define(['knockout', '../services/apiService', 'ojs/ojknockout', 'ojs/ojbutton', 'ojs/ojinputtext'],
  function(ko, apiService) {
    'use strict';

    function LoginViewModel(params) {
      const self = this;

      // Active Tab: 'login' | 'register' | 'forgot'
      this.activeTab = ko.observable('login');
      this.errorMessage = ko.observable('');
      this.successMessage = ko.observable('');
      this.isLoading = ko.observable(false);

      // Sign In Form
      this.email = ko.observable('');
      this.password = ko.observable('');
      this.otp = ko.observable('');
      this.mfaRequired = ko.observable(false);

      // Registration Form
      this.regEmail = ko.observable('');
      this.regPassword = ko.observable('');

      // Forgot / Reset Password Form
      this.forgotEmail = ko.observable('');
      this.resetToken = ko.observable('');
      this.newPassword = ko.observable('');
      this.resetStep = ko.observable(1); // 1: request token, 2: set new password

      this.clearMessages = () => {
        self.errorMessage('');
        self.successMessage('');
      };

      this.switchTab = (tab) => {
        self.clearMessages();
        self.activeTab(tab);
      };

      // -----------------------------------------------------------
      // Sign In Action
      // -----------------------------------------------------------
      this.handleLogin = async () => {
        self.clearMessages();
        if (!self.email() || !self.password()) {
          self.errorMessage('Please enter both email and password.');
          return;
        }

        self.isLoading(true);
        try {
          const res = await apiService.login(self.email().trim(), self.password(), self.otp() || null);
          if (res.mfaRequired) {
            self.mfaRequired(true);
            self.successMessage('MFA Required: Enter the OTP dispatched to your registered email.');
          } else {
            self.successMessage('Authentication successful! Loading your portal...');
            if (apiService.isAdmin()) {
              apiService.navigate('admin');
            } else {
              apiService.navigate('dashboard');
            }
          }
        } catch (err) {
          self.errorMessage(err.message || 'Login failed. Please check your credentials.');
        } finally {
          self.isLoading(false);
        }
      };

      // -----------------------------------------------------------
      // Register Action
      // -----------------------------------------------------------
      this.handleRegister = async () => {
        self.clearMessages();
        if (!self.regEmail() || !self.regPassword()) {
          self.errorMessage('Please provide an email and password.');
          return;
        }

        self.isLoading(true);
        try {
          const res = await apiService.register(self.regEmail().trim(), self.regPassword());
          self.successMessage(res.message || 'Registration successful! You can now sign in.');
          self.email(self.regEmail());
          self.password(self.regPassword());
          setTimeout(() => {
            self.switchTab('login');
          }, 1500);
        } catch (err) {
          self.errorMessage(err.message || 'Registration failed.');
        } finally {
          self.isLoading(false);
        }
      };

      // -----------------------------------------------------------
      // Forgot Password Actions (Token hidden from user)
      // -----------------------------------------------------------
      this.confirmNewPassword = ko.observable('');

      this.handleForgotPassword = async () => {
        self.clearMessages();
        if (!self.forgotEmail()) {
          self.errorMessage('Please enter your registered email address.');
          return;
        }

        self.isLoading(true);
        try {
          const res = await apiService.forgotPassword(self.forgotEmail().trim());
          if (res.resetToken) {
            self.resetToken(res.resetToken);
            self.resetStep(2);
            self.successMessage('Password reset authorized! Please enter your new password below.');
          } else {
            self.successMessage(res.message || 'Password reset instructions issued.');
          }
        } catch (err) {
          self.errorMessage(err.message || 'Failed to request password reset.');
        } finally {
          self.isLoading(false);
        }
      };

      this.handleResetPassword = async () => {
        self.clearMessages();
        if (!self.newPassword()) {
          self.errorMessage('Please enter your new password.');
          return;
        }
        if (self.confirmNewPassword() && self.newPassword() !== self.confirmNewPassword()) {
          self.errorMessage('Passwords do not match. Please verify your new password.');
          return;
        }

        self.isLoading(true);
        try {
          const res = await apiService.resetPassword(self.resetToken().trim(), self.newPassword());
          self.successMessage(res.message || 'Password reset successful! Please sign in with your new password.');
          self.email(self.forgotEmail());
          self.password(self.newPassword());
          setTimeout(() => {
            self.resetStep(1);
            self.switchTab('login');
          }, 1500);
        } catch (err) {
          self.errorMessage(err.message || 'Password reset failed. Please request a new reset verification.');
        } finally {
          self.isLoading(false);
        }
      };

      // Lifecycle hooks
      this.connected = () => {
        document.title = 'Sign In - NetBanking Portal';
      };
    }

    return LoginViewModel;
  }
);
