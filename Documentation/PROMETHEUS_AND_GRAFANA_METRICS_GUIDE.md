# NetBanking Observability: Prometheus & Grafana Metrics Guide

## 1. Overview

This document details the business and operational metrics implemented across all NetBanking microservices. These metrics track every function and user flow performed through the Oracle JET Redwood frontend portal, exposed via Spring Boot Actuator (`/actuator/prometheus`) and scraped by Prometheus.

A ready-to-import Grafana dashboard has been created at:
[`Monitoring/netbanking-frontend-operations-dashboard.json`](file:///c:/Users/Varshit%20G/Documents/Project/Monitoring/netbanking-frontend-operations-dashboard.json)

---

## 2. Microservice Metrics Catalog

### 2.1. Authentication Service (`auth-service` :8081)

Tracks user and admin credentials login, MFA OTP generation, OTP verification, customer self-registration, session renewals, and logout actions.

| Prometheus Metric Name | Type | Labels / Tags | Description | Frontend User Action |
|---|---|---|---|---|
| `netbanking_auth_login_requests_total` | Counter | `role` (`CUSTOMER`, `ADMIN`), `status` (`SUCCESS`, `FAILURE`, `LOCKED`, `MFA_REQUIRED`), `reason` (`AUTHENTICATED`, `OTP_CHALLENGE_ISSUED`, `BAD_CREDENTIALS`, `USER_NOT_FOUND`, etc.) | Total login attempts initiated from the frontend | Sign-In form submission |
| `netbanking_auth_login_duration_seconds` | Timer | Standard Micrometer timer percentiles & count/sum | Execution latency of authentication check | Login latency |
| `netbanking_auth_otp_issued_total` | Counter | `purpose` (`LOGIN`, `EMAIL_VERIFICATION`, `PASSWORD_RESET`) | Total 6-digit verification codes generated and published to Kafka | Login MFA challenge or Resend OTP button |
| `netbanking_auth_otp_verified_total` | Counter | `purpose` (`LOGIN`, `EMAIL_VERIFICATION`, `PASSWORD_RESET`), `status` (`SUCCESS`, `INVALID`, `EXPIRED`, `LIMIT_EXCEEDED`, `OTP_NOT_FOUND`) | Result of OTP submissions | OTP verification modal |
| `netbanking_auth_registration_requests_total` | Counter | `status` (`SUCCESS`, `FAILURE_EMAIL_EXISTS`) | Self-registration requests | "Open Account / Sign Up" form |
| `netbanking_auth_token_refresh_total` | Counter | `status` (`SUCCESS`, `FAILURE`) | Background token refresh calls keeping customer session active | Session refresh / page transition |
| `netbanking_auth_logout_requests_total` | Counter | None | Session revocation requests | "Sign Out" button |
| `netbanking_auth_password_reset_total` | Counter | `step` (`REQUEST`, `RESET`), `status` (`SUCCESS`, `FAILURE_*`) | Forgot password requests and token resets | Forgot Password flow |

---

### 2.2. User & Customer Profile Service (`User-Service` :8082)

Tracks customer KYC profile creation, inspection, administrative customer approvals/rejections, and account opening governance requests.

| Prometheus Metric Name | Type | Labels / Tags | Description | Frontend User Action |
|---|---|---|---|---|
| `netbanking_customer_profile_requests_total` | Counter | `action` (`CREATE`, `GET`, `UPDATE`), `status` (`SUCCESS`, `NOT_FOUND`) | Customer KYC profile operations | Profile Setup / My Profile tab |
| `netbanking_customer_status_changes_total` | Counter | `target_status` (`ACTIVE`, `BLOCKED`, `REJECTED`, `INACTIVE`), `status` (`SUCCESS`, `FAILURE`) | Admin approvals and status updates on customer accounts | Admin Customer Governance |
| `netbanking_customer_account_opening_requests_total` | Counter | `action` (`SUBMIT`, `APPROVE`, `REJECT`), `account_type` (`SAVINGS`, `CURRENT`, `ALL`), `status` (`SUCCESS`, `FAILURE`) | Account opening workflow requests and admin reviews | Customer Apply for Account / Admin Approval |
| `netbanking_customer_governance_queries_total` | Counter | `scope` (`ALL_CUSTOMERS`, `CUSTOMER_INSPECT`, `PENDING_ONBOARDING`) | Directory and inspection lookups | Admin Account Governance & Inspector |

---

### 2.3. Account & Ledger Service (`Account-Service` :8083)

Tracks bank account creation, balance checks, cash deposits, debits, 4-digit security PIN changes, and ledger queries.

| Prometheus Metric Name | Type | Labels / Tags | Description | Frontend User Action |
|---|---|---|---|---|
| `netbanking_account_create_requests_total` | Counter | `type` (`SAVINGS`, `CURRENT`), `status` (`SUCCESS`, `FAILURE`) | Bank accounts opened | New Account Creation |
| `netbanking_account_fetch_requests_total` | Counter | `query` (`SINGLE`, `BY_CUSTOMER`, `ALL`) | Account lookups and lists | Dashboard accounts dropdown, Admin list |
| `netbanking_account_balance_checks_total` | Counter | `status` (`SUCCESS`, `FAILURE`) | Live balance lookups | Dashboard account balances & Transfer source check |
| `netbanking_account_deposit_requests_total` | Counter | `status` (`SUCCESS`, `FAILURE`) | Deposit operations | Admin & Customer Cash Deposit |
| `netbanking_account_deposit_amount_inr` | DistributionSummary | `baseUnit: INR` | Value of deposits processed in INR | Deposit amount tracking |
| `netbanking_account_debit_requests_total` | Counter | `status` (`SUCCESS`, `FAILURE`) | Debit postings (withdrawals, transfers) | Outgoing transaction postings |
| `netbanking_account_status_changes_total` | Counter | `new_status` (`ACTIVE`, `FROZEN`, `BLOCKED`, `CLOSED`), `status` (`SUCCESS`, `FAILURE`) | Account freeze/block/unblock actions | Admin Set Status modal |
| `netbanking_account_pin_operations_total` | Counter | `action` (`SET`, `UPDATE`, `VERIFY`), `status` (`SUCCESS`, `INVALID`, `FAILURE`) | 4-digit security PIN operations | Set/Update PIN modal & Transfer PIN verification |
| `netbanking_account_ledger_queries_total` | Counter | `status` (`SUCCESS`, `FAILURE`) | Ledger statement queries | Live 10-column Ledger page |

---

### 2.4. Transaction & Transfer Service (`Transaction-service` :8084)

Tracks fund transfers (Internal, External, Self), transfer amounts, latency, transaction history inquiries, and statement generations.

| Prometheus Metric Name | Type | Labels / Tags | Description | Frontend User Action |
|---|---|---|---|---|
| `netbanking_transfer_requests_total` | Counter | `type` (`INTERNAL`, `EXTERNAL`, `SELF`), `status` (`SUCCESS`, `FAILURE`) | Fund transfers executed from the portal | Fund Transfer submit button |
| `netbanking_transfer_amount_inr` | DistributionSummary | `baseUnit: INR` | Volume of funds transferred in Indian Rupees (INR) | Transfer transaction volume |
| `netbanking_transfer_duration_seconds` | Timer | Standard Micrometer timer percentiles & count/sum | End-to-end execution latency of transfers | Transfer latency monitoring |
| `netbanking_transaction_history_queries_total` | Counter | `scope` (`BY_CUSTOMER`, `BY_ACCOUNT`, `BY_REFERENCE`, `ALL`), `status` (`SUCCESS`, `FAILURE`) | Transaction history and audit search lookups | Transaction History page & filters |
| `netbanking_statement_requests_total` | Counter | `action` (`REQUEST`, `STATUS`, `LIST`, `DOWNLOAD`), `status` (`SUCCESS`, `FAILURE`) | Account statement operations | Account Statement generation & Download button |
| `netbanking_statement_download_duration_seconds` | Timer | Standard Micrometer timer | Time taken to generate and download statement content | Statement download performance |

---

### 2.5. Notification Service (`Notification-Service` :8085)

Tracks customer notification inquiries, email dispatches via Google SMTP, delivery latency, and audit trail passkey access.

| Prometheus Metric Name | Type | Labels / Tags | Description | Frontend User Action |
|---|---|---|---|---|
| `netbanking_notification_fetch_requests_total` | Counter | `scope` (`ALL`, `BY_CUSTOMER`, `BY_ID`) | Notification center queries | Notification bell dropdown |
| `netbanking_notification_email_dispatched_total` | Counter | `type` (`OTP`, `WELCOME`, `TRANSFER_DEBIT`, `TRANSFER_CREDIT`, `TRANSFER_FAILED`, `GENERAL`), `status` (`SUCCESS`, `FAILURE`) | Customer emails dispatched via Google SMTP | Email alerts on OTP, Transfers, etc. |
| `netbanking_notification_email_duration_seconds` | Timer | Standard Micrometer timer | Latency of SMTP dispatch | Google SMTP health and responsiveness |
| `netbanking_audit_access_requests_total` | Counter | `action` (`REQUEST`, `APPROVE`), `status` (`SUCCESS`, `FAILURE`) | Audit passkey requests and administrative approvals | Audit log passkey generation flow |

---

## 3. How to View in Grafana

1. Open your running Grafana instance: [http://localhost:3000](http://localhost:3000).
2. Go to **Dashboards** &rarr; **New** &rarr; **Import**.
3. Choose **Upload JSON file** and select:
   `c:\Users\Varshit G\Documents\Project\Monitoring\netbanking-frontend-operations-dashboard.json`
4. Select your **Prometheus** data source and click **Import**.
5. You will see live, real-time charts organized into 5 sections reflecting all actions performed in the NetBanking application!

---

## 4. Useful Prometheus PromQL Queries

### Check Authentication Success vs Failure:
```promql
sum by (status) (rate(netbanking_auth_login_requests_total[5m]))
```

### Check OTP Verification Rate:
```promql
sum by (purpose, status) (increase(netbanking_auth_otp_verified_total[5m]))
```

### Total Money Transferred (INR):
```promql
sum(netbanking_transfer_amount_inr_sum)
```

### Average Transfer Execution Latency (seconds):
```promql
rate(netbanking_transfer_duration_seconds_sum[5m]) / rate(netbanking_transfer_duration_seconds_count[5m])
```

### Email Notifications Sent via Google SMTP:
```promql
sum by (type, status) (netbanking_notification_email_dispatched_total)
```
