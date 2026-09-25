/**
 * Notifications ViewModel
 * Displays real-time email dispatch records, security alerts, and system audit logs.
 */
define(['knockout', '../services/apiService', 'ojs/ojknockout', 'ojs/ojbutton'],
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
