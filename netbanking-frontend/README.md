# NetBanking Enterprise Frontend - Oracle JET & Redwood

A complete, modern, responsive online banking portal for the NetBanking microservices platform, built using **Oracle JavaScript Extension Toolkit (Oracle JET)** and styled with Oracle's official **Redwood Design System**.

---

## Architecture & Integration

* **Frontend Framework**: Oracle JavaScript Extension Toolkit (Oracle JET) with Knockout MVVM architecture.
* **Design System**: Oracle Redwood Theme (`oj-redwood`) with official Oracle UX icon fonts.
* **API Gateway Connectivity**: Connects to the Spring Cloud API Gateway at `http://localhost:8080`.
* **Port**: Runs on `http://localhost:8000`.

```
[Browser Client] -> [Oracle JET Redwood Frontend :8000]
                           |
                           v
                [Spring Cloud API Gateway :8080]
                           |
    +--------------+-------+-------+---------------+
    |              |               |               |
[Auth-Service] [User-Service] [Account-Service] [Transaction-Service] [Notification-Service]
    :8081          :8082           :8083             :8084                  :8085
```

---

## Features & Modules

### 1. Authentication & Security (`#login`)
* **Sign In**: Email & password authentication, handles 401s, auto token refresh, and MFA OTP verification when required.
* **Open Account**: Self-service registration for new customers.
* **Forgot / Reset Password**: Step-by-step password reset utilizing the 15-minute temporary reset token.
* **Role Recognition**: Automatically detects and activates `ADMIN` privileges based on JWT claims.

### 2. Customer Dashboard (`#dashboard`)
* **Redwood Metric KPI Cards**: Real-time display of Total Available Net Worth (in ₹ INR), Active Accounts count, Pending Applications, and Security status.
* **Accounts Summary Table**: Quick view of account numbers, types (`SAVINGS` / `CURRENT`), statuses (`ACTIVE`, `PENDING_APPROVAL`, `FROZEN`), and balances.
* **Quick Actions**: One-click shortcuts to initiate transfers, make deposits, or request new accounts.

### 3. Account Management (`#accounts`)
* **Detailed Account Cards**: Available vs. Current (ledger) balances, account opening timestamps, and currency code.
* **4-Digit Security PIN**: Set and update the 4-digit transaction authorization PIN directly with Account-Service.
* **Deposit Funds**: Credit ledger postings with custom reference descriptions.
* **Account Type & Status Filters**: Filter by `SAVINGS`, `CURRENT`, `ACTIVE`, or `PENDING_APPROVAL`.

### 4. Fund Transfers (`#transfers`)
* **Intra-bank Transfers**: Source account selector showing live available balance.
* **PIN Authorization**: Transfers strictly require the account's 4-digit PIN before execution.
* **Transfer Receipts**: Displays transaction references, debited amounts, and updated balances upon completion.

### 5. Administrator Portal (`#admin`)
* **Role-Gated**: Visible and accessible only to users with the `ADMIN` role.
* **Pending KYC & Onboarding Queue**: Real-time review of customer onboarding requests.
* **One-Click Provisioning**: Approving an application automatically triggers OpenFeign to provision an active checking/savings account in Account-Service.
* **Direct Account Status Override**: Instantly transition any account between `ACTIVE`, `PENDING_APPROVAL`, `FROZEN`, `BLOCKED`, and `CLOSED`.
* **Compliance Passkey**: One-click request for 5-minute single-use audit access passkeys.

### 6. Notifications Center (`#notifications`)
* Real-time activity feed and audit trail of OTP codes, login events, and email dispatches.

---

## Quick Start

### Starting the Frontend Manually
From the project root:
```powershell
cd netbanking-frontend
node server.js
```
Open **`http://localhost:8000`** in your browser.

### Starting Everything Together
Double-click `start_netbanking.bat` or run:
```powershell
.\start_netbanking.ps1
```
This automatically starts:
1. Podman infrastructure containers (Oracle DB, Kafka, Redis)
2. Eureka Discovery Server (`:8761`)
3. Auth, User, Account, Transaction, and Notification microservices (`:8081` - `:8085`)
4. API Gateway (`:8080`)
5. Oracle JET Redwood Frontend (`:8000`)
