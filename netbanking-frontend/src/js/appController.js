/**
 * NetBanking Redwood App Controller
 * Manages global navigation, responsive drawer, user authentication state, and Redwood page header.
 */
define(['knockout', './services/apiService', 'ojs/ojcontext', 'ojs/ojmodule-element-utils', 'ojs/ojknockouttemplateutils', 'ojs/ojcorerouter', 'ojs/ojmodulerouter-adapter', 'ojs/ojknockoutrouteradapter', 'ojs/ojurlparamadapter', 'ojs/ojresponsiveutils', 'ojs/ojresponsiveknockoututils', 'ojs/ojarraydataprovider',
        'ojs/ojdrawerpopup', 'ojs/ojmodule-element', 'ojs/ojknockout', 'ojs/ojbutton'],
  function(ko, apiService, Context, moduleUtils, KnockoutTemplateUtils, CoreRouter, ModuleRouterAdapter, KnockoutRouterAdapter, UrlParamAdapter, ResponsiveUtils, ResponsiveKnockoutUtils, ArrayDataProvider) {

    function ControllerViewModel() {
      const self = this;
      this.KnockoutTemplateUtils = KnockoutTemplateUtils;

      // Accessibility Announcements
      this.manner = ko.observable('polite');
      this.message = ko.observable();
      const announcementHandler = (event) => {
        this.message(event.detail.message);
        this.manner(event.detail.manner);
      };
      const globalBody = document.getElementById('globalBody');
      if (globalBody) {
        globalBody.addEventListener('announce', announcementHandler, false);
      }

      // Responsive media queries
      const smQuery = ResponsiveUtils.getFrameworkQuery(ResponsiveUtils.FRAMEWORK_QUERY_KEY.SM_ONLY);
      this.smScreen = ResponsiveKnockoutUtils.createMediaQueryObservable(smQuery);
      const mdQuery = ResponsiveUtils.getFrameworkQuery(ResponsiveUtils.FRAMEWORK_QUERY_KEY.MD_UP);
      this.mdScreen = ResponsiveKnockoutUtils.createMediaQueryObservable(mdQuery);

      // User & Auth State Observables
      this.isLoggedIn = ko.observable(apiService.isAuthenticated());
      this.isAdmin = ko.observable(apiService.isAdmin());
      this.currentUser = ko.observable(apiService.getUser());
      this.userLogin = ko.observable(
        this.currentUser() ? (this.currentUser().email || this.currentUser().customerId) : 'Guest'
      );
      this.userBadge = ko.computed(() => {
        if (!self.isLoggedIn()) return 'Signed Out';
        return self.isAdmin() ? 'ADMIN' : 'CUSTOMER';
      });

      // Navigation Routes Definition
      this.getAllNavData = () => {
        return [
          { path: '', redirect: self.isAdmin() ? 'admin' : (self.isLoggedIn() ? 'dashboard' : 'login') },
          { path: 'admin', detail: { label: 'Admin Command Center', iconClass: 'oj-ux-ico-dashboard' } },
          { path: 'dashboard', detail: { label: 'Customer Dashboard', iconClass: 'oj-ux-ico-bar-chart' } },
          { path: 'accounts', detail: { label: 'My Accounts', iconClass: 'oj-ux-ico-credit-card' } },
          { path: 'transfers', detail: { label: 'Fund Transfers', iconClass: 'oj-ux-ico-exchange' } },
          { path: 'transactions', detail: { label: 'Transaction History', iconClass: 'oj-ux-ico-history' } },
          { path: 'notifications', detail: { label: self.isAdmin() ? 'System Audit Logs' : 'Notifications', iconClass: 'oj-ux-ico-bell' } },
          { path: 'login', detail: { label: 'Sign In', iconClass: 'oj-ux-ico-lock' } }
        ];
      };

      // Active routes observable for nav list (STRICT ROLE ISOLATION)
      this.getDisplayNavData = () => {
        if (!self.isLoggedIn()) {
          return [{ path: 'login', detail: { label: 'Sign In', iconClass: 'oj-ux-ico-lock' } }];
        }

        // Admin sees ONLY administrative operations:
        if (self.isAdmin()) {
          return [
            { path: 'admin', detail: { label: 'Admin Command Center', iconClass: 'oj-ux-ico-dashboard' } },
            { path: 'notifications', detail: { label: 'System Audit Logs', iconClass: 'oj-ux-ico-bell' } }
          ];
        }

        // Retail customer sees customer banking features:
        return [
          { path: 'dashboard', detail: { label: 'My Dashboard', iconClass: 'oj-ux-ico-bar-chart' } },
          { path: 'accounts', detail: { label: 'My Accounts', iconClass: 'oj-ux-ico-credit-card' } },
          { path: 'transfers', detail: { label: 'Fund Transfers', iconClass: 'oj-ux-ico-exchange' } },
          { path: 'transactions', detail: { label: 'Transactions & Statements', iconClass: 'oj-ux-ico-history' } },
          { path: 'notifications', detail: { label: 'Notifications', iconClass: 'oj-ux-ico-bell' } }
        ];
      };

      const initialRoutes = this.getAllNavData();
      this.router = new CoreRouter(initialRoutes, {
        urlAdapter: new UrlParamAdapter()
      });
      apiService.setRouter(this.router);

      this.moduleAdapter = new ModuleRouterAdapter(this.router);
      this.selection = new KnockoutRouterAdapter(this.router);
      this.navDataProvider = ko.observable(new ArrayDataProvider(this.getDisplayNavData(), { keyAttributes: 'path' }));

      // Listen for router transitions to protect authenticated and role-based routes
      this.router.beforeStateChange.subscribe((change) => {
        const targetPath = change.state ? change.state.path : '';
        if (!targetPath || targetPath === 'login') return;

        if (!apiService.isAuthenticated()) {
          change.accept(Promise.reject('Authentication required'));
          setTimeout(() => { self.router.go({ path: 'login' }); }, 0);
          return;
        }

        // Protect Admin view from non-admin customers
        if (targetPath === 'admin' && !apiService.isAdmin()) {
          change.accept(Promise.reject('Admin privilege required'));
          setTimeout(() => { self.router.go({ path: 'dashboard' }); }, 0);
          return;
        }

        // Protect Admin from being diverted into customer retail views
        if ((targetPath === 'dashboard' || targetPath === 'accounts' || targetPath === 'transfers' || targetPath === 'transactions') && apiService.isAdmin()) {
          change.accept(Promise.reject('Redirecting admin to Admin Command Center'));
          setTimeout(() => { self.router.go({ path: 'admin' }); }, 0);
          return;
        }
      });

      this.router.sync();

      // Direct navigation helper
      this.goTo = (path) => {
        self.router.go({ path: path });
      };

      // Side Drawer
      this.sideDrawerOn = ko.observable(false);
      this.mdScreen.subscribe(() => { self.sideDrawerOn(false); });
      this.toggleDrawer = () => {
        self.sideDrawerOn(!self.sideDrawerOn());
      };

      // Header Branding
      this.appName = ko.observable('NetBanking Redwood Portal');

      // Update UI on Auth State Change
      apiService.onAuthChange((user) => {
        self.isLoggedIn(!!user);
        self.isAdmin(apiService.isAdmin());
        self.currentUser(user);
        self.userLogin(user ? (user.email || user.customerId) : 'Guest');
        self.navDataProvider(new ArrayDataProvider(self.getDisplayNavData(), { keyAttributes: 'path' }));

        if (!user) {
          self.router.go({ path: 'login' });
        } else if (self.isAdmin()) {
          self.router.go({ path: 'admin' });
        } else {
          self.router.go({ path: 'dashboard' });
        }
      });

      // Actions
      this.logout = async () => {
        await apiService.logout();
        self.router.go({ path: 'login' });
      };

      this.goToNotifications = () => {
        self.router.go({ path: 'notifications' });
      };

      // Footer Links
      this.footerLinks = [
        { name: 'API Gateway (8080)', linkId: 'gateway', linkTarget: 'http://localhost:8080/actuator/health' },
        { name: 'Swagger Docs', linkId: 'swagger', linkTarget: 'http://localhost:8081/swagger-ui/index.html' },
        { name: 'Eureka Dashboard', linkId: 'eureka', linkTarget: 'http://localhost:8761' },
        { name: 'Security Notice', linkId: 'security', linkTarget: '#' }
      ];
    }

    Context.getPageContext().getBusyContext().applicationBootstrapComplete();
    return new ControllerViewModel();
  }
);
