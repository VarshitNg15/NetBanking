# NetBanking Platform: Complete API Reference Manual

This document serves as the master specification of **all REST API endpoints** across the NetBanking microservices architecture. It includes URL paths, HTTP methods, headers, authentication requirements, request/response bodies, status codes, and terminal cURL examples.

---

## Table of Contents
1. [Architecture & Edge Gateway Security](#1-architecture--edge-gateway-security)
2. [Service Port & Gateway Route Mapping](#2-service-port--gateway-route-mapping)
3. [Authentication Service (`auth-service`)](#3-authentication-service-auth-service)
4. [User & Customer Service (`user-service`)](#4-user--customer-service-user-service)
5. [Account & Ledger Service (`account-service`)](#5-account--ledger-service-account-service)
6. [Transaction & Statement Service (`transaction-service`)](#6-transaction--statement-service-transaction-service)
7. [Notification & Audit Service (`notification-service`)](#7-notification--audit-service-notification-service)
8. [Edge Gateway & Service Discovery Endpoints](#8-edge-gateway--service-discovery-endpoints)
9. [Inter-Service Communication (OpenFeign & Kafka)](#9-inter-service-communication-openfeign--kafka)

---

## 1. Architecture & Edge Gateway Security

The NetBanking platform employs a **Perimeter Security Model**:
- **Public Traffic** routes through the **Spring Cloud API Gateway** on port `8080`.
- **Public Endpoints** (such as registration, login, OTP verification, password reset, and health checks) bypass token verification.
- **Protected Endpoints** require a valid JWT Bearer token:
  ```http
  Authorization: Bearer <JWT_ACCESS_TOKEN>
  ```
- **Context Injection**: Upon validating the JWT, the Gateway strips the client token and injects trusted context headers downstream:
  - `X-Customer-Id`: User's unique customer ID (e.g., `C1A2B3C4D5E6`)
  - `X-User-Email`: Authenticated user's email address
  - `X-User-Roles`: Comma-separated user roles (e.g., `ROLE_CUSTOMER,ROLE_ADMIN`)
- **Rate Limiting**: Configured per route via Redis token-bucket filter (returns HTTP `429 Too Many Requests` when exceeded).

---

## 2. Service Port & Gateway Route Mapping

| Microservice | Direct Port | Gateway Path Prefix | Rate Limit (Replenish / Burst) |
| :--- | :---: | :--- | :---: |
| **API Gateway** | `8080` | `http://localhost:8080` | N/A |
| **Eureka Server** | `8761` | `http://localhost:8761` | N/A |
| **Auth Service** | `8081` | `/api/v1/auth/**`, `/api/auth/**` | 5 / 10 req/s |
| **User Service** | `8082` | `/api/v1/customers/**`, `/api/v1/users/**` | 10 / 20 req/s |
| **Account Service** | `8083` | `/api/v1/accounts/**`, `/api/accounts/**` | 5 / 10 req/s |
| **Transaction Service** | `8084` | `/api/v1/transactions/**`, `/api/v1/statements/**` | 3 / 6 req/s |
| **Notification Service**| `8085` | `/api/v1/notifications/**`, `/api/v1/audit/**` | 5 / 10 req/s |

---

## 3. Authentication Service (`auth-service`)

Manages user identity, credentials, BCrypt password hashing, JWT token lifecycle, MFA OTP generation, and password resets.

### Endpoint Summary
| Method | Endpoint | Access | Summary |
| :--- | :--- | :---: | :--- |
| `POST` | `/api/v1/auth/register` | Public | Register new user identity & trigger verification OTP |
| `POST` | `/api/v1/auth/login` | Public | Authenticate user (returns JWT or MFA OTP challenge) |
| `POST` | `/api/v1/auth/refresh` | Public | Rotate refresh token and issue new access token |
| `POST` | `/api/v1/auth/logout` | Public | Revoke refresh token and terminate session |
| `POST` | `/api/v1/auth/verify-otp` | Public | Validate 6-digit OTP code |
| `POST` | `/api/v1/auth/forgot-password`| Public | Request password reset token / email |
| `POST` | `/api/v1/auth/reset-password` | Public | Set new password using reset token |

---

### 3.1. Register User
- **Method**: `POST`
- **Path**: `/api/v1/auth/register`
- **Description**: Creates a new user record in `AUTH_SCHEMA.USERS`, assigns `ROLE_CUSTOMER`, initializes customer shell in `User-Service`, and dispatches an email verification OTP via Kafka.
- **Request Body**:
  ```json
  {
    "email": "customer@example.com",
    "password": "StrongPassword123!"
  }
  ```
- **Response (`200 OK`)**:
  ```json
  {
    "message": "Registration successful. An email verification OTP has been issued."
  }
  ```
- **cURL Example**:
  ```bash
  curl -X POST "http://localhost:8080/api/v1/auth/register" \
    -H "Content-Type: application/json" \
    -d '{"email":"customer@example.com","password":"StrongPassword123!"}'
  ```

---

### 3.2. Login / Authenticate
- **Method**: `POST`
- **Path**: `/api/v1/auth/login`
- **Description**: Validates email and password. If MFA is active (`mfaEnabled="Y"`), dispatches a 6-digit OTP to the email and returns `mfaRequired: true`. If OTP is provided or MFA is disabled, returns JWT tokens.
- **Request Body (Step 1 - Initial Challenge)**:
  ```json
  {
    "email": "customer@example.com",
    "password": "StrongPassword123!"
  }
  ```
- **Response (`200 OK` - MFA Required)**:
  ```json
  {
    "accessToken": null,
    "refreshToken": null,
    "tokenType": "Bearer",
    "expiresIn": 0,
    "customerId": "C1001",
    "roles": ["CUSTOMER"],
    "mfaRequired": true
  }
  ```
- **Request Body (Step 2 - Complete with OTP)**:
  ```json
  {
    "email": "customer@example.com",
    "password": "StrongPassword123!",
    "otp": "491823"
  }
  ```
- **Response (`200 OK` - Full Token Response)**:
  ```json
  {
    "accessToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
    "refreshToken": "48b17ce2-9014-4113-a442-f90b8417c800...",
    "tokenType": "Bearer",
    "expiresIn": 3600,
    "customerId": "C1001",
    "roles": ["CUSTOMER"],
    "mfaRequired": false
  }
  ```

---

### 3.3. Verify OTP
- **Method**: `POST`
- **Path**: `/api/v1/auth/verify-otp`
- **Description**: Validates a 6-digit OTP code against the BCrypt hash in `AUTH_SCHEMA.EMAIL_OTPS`. Purpose can be `EMAIL_VERIFICATION`, `LOGIN`, or `PASSWORD_RESET`.
- **Request Body**:
  ```json
  {
    "email": "customer@example.com",
    "otp": "491823",
    "purpose": "EMAIL_VERIFICATION"
  }
  ```
- **Response (`200 OK`)**:
  ```json
  {
    "message": "OTP verified successfully"
  }
  ```

---

### 3.4. Refresh Access Token
- **Method**: `POST`
- **Path**: `/api/v1/auth/refresh`
- **Description**: Exchanges a valid refresh token for a newly signed JWT and rotated refresh token.
- **Request Body**:
  ```json
  {
    "refreshToken": "48b17ce2-9014-4113-a442-f90b8417c800"
  }
  ```
- **Response (`200 OK`)**: Same as TokenResponse schema.

---

### 3.5. Logout
- **Method**: `POST`
- **Path**: `/api/v1/auth/logout`
- **Description**: Revokes the refresh token and marks the session as `LOGOUT` in history.
- **Request Body**:
  ```json
  {
    "refreshToken": "48b17ce2-9014-4113-a442-f90b8417c800"
  }
  ```
- **Response (`200 OK`)**:
  ```json
  {
    "message": "Logout successful"
  }
  ```

---

### 3.6. Forgot Password
- **Method**: `POST`
- **Path**: `/api/v1/auth/forgot-password`
- **Description**: Generates a temporary 15-minute access token specifically authorized only for password reset (no Kafka notification containing the token is dispatched).
- **Request Body**:
  ```json
  {
    "email": "customer@example.com"
  }
  ```
- **Response (`200 OK`)**:
  ```json
  {
    "message": "Temporary password reset token generated successfully. Valid for 15 minutes.",
    "resetToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiJDMTAwMSIsInB1cnBvc2UiOiJQQVNTV09SRF9SRVNFVCIsInJvbGVzIjpbIlJPTEVfUkVTRVRfUEFTU1dPUkQiXX0...",
    "token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiJDMTAwMSIsInB1cnBvc2UiOiJQQVNTV09SRF9SRVNFVCIsInJvbGVzIjpbIlJPTEVfUkVTRVRfUEFTU1dPUkQiXX0...",
    "tokenType": "Bearer",
    "expiresIn": 900
  }
  ```

---

### 3.7. Reset Password
- **Method**: `POST`
- **Path**: `/api/v1/auth/reset-password`
- **Description**: Consumes a valid reset token and replaces the password hash.
- **Request Body**:
  ```json
  {
    "token": "d98124b8-...",
    "newPassword": "BrandNewPassword456!"
  }
  ```
- **Response (`200 OK`)**:
  ```json
  {
    "message": "Password reset successful"
  }
  ```

---

## 4. User & Customer Service (`user-service`)

Manages customer records, KYC profiles, onboarding requests, and administrative account opening approvals.

### Endpoint Summary
| Method | Endpoint | Access | Summary |
| :--- | :--- | :---: | :--- |
| `POST` | `/api/v1/customers` | Protected | Create a new customer master record |
| `GET` | `/api/v1/customers/{customerId}` | Protected | Retrieve customer record by Customer ID |
| `GET` | `/api/v1/customers` | Protected | Retrieve all customers |
| `PATCH`| `/api/v1/customers/{customerId}/status` | Protected | Update customer status (ACTIVE, SUSPENDED) |
| `POST` | `/api/v1/customers/{customerId}/profile` | Protected | Create customer KYC profile |
| `GET` | `/api/v1/customers/{customerId}/profile` | Protected | Retrieve customer profile |
| `PUT` | `/api/v1/customers/{customerId}/profile` | Protected | Update customer profile |
| `POST` | `/api/v1/users/account-opening` | Protected | Submit account opening request |
| `GET` | `/api/v1/users/account-opening/{requestId}` | Protected | Get account request by Request ID |
| `GET` | `/api/v1/users/account-opening/customer/{customerId}` | Protected | Get all account requests for a customer |
| `GET` | `/api/v1/users/admin/account-opening/pending` | Admin | Get pending account opening requests |
| `POST` | `/api/v1/users/admin/account-opening/{requestId}/approve` | Admin | Approve request and provision account |
| `POST` | `/api/v1/users/admin/account-opening/{requestId}/reject` | Admin | Reject account opening request |

---

### 4.1. Create Customer
- **Method**: `POST`
- **Path**: `/api/v1/customers`
- **Headers**: `Authorization: Bearer <JWT>`
- **Request Body**:
  ```json
  {
    "customerId": "C1001"
  }
  ```
- **Response (`201 Created`)**:
  ```json
  {
    "customerId": "C1001",
    "status": "PENDING_APPROVAL",
    "createdAt": "2026-09-18T10:00:00"
  }
  ```

---

### 4.2. Get Customer by ID
- **Method**: `GET`
- **Path**: `/api/v1/customers/{customerId}`
- **Headers**: `Authorization: Bearer <JWT>`
- **Response (`200 OK`)**:
  ```json
  {
    "customerId": "C1001",
    "status": "ACTIVE",
    "createdAt": "2026-09-18T10:00:00",
    "updatedAt": "2026-09-18T10:05:00"
  }
  ```

---

### 4.3. Update Customer Status
- **Method**: `PATCH`
- **Path**: `/api/v1/customers/{customerId}/status?status=ACTIVE`
- **Headers**: `Authorization: Bearer <JWT>`
- **Response (`200 OK`)**: Returns updated CustomerResponse.

---

### 4.4. Create Customer Profile
- **Method**: `POST`
- **Path**: `/api/v1/customers/{customerId}/profile`
- **Headers**: `Authorization: Bearer <JWT>`
- **Request Body**:
  ```json
  {
    "firstName": "John",
    "lastName": "Doe",
    "dateOfBirth": "1990-05-15",
    "phoneNumber": "+919876543210",
    "addressLine1": "123 Main Street",
    "addressLine2": "Apt 4B",
    "city": "Bengaluru",
    "state": "Karnataka",
    "postalCode": "560001",
    "country": "India"
  }
  ```
- **Response (`201 Created`)**:
  ```json
  {
    "customerId": "C1001",
    "firstName": "John",
    "lastName": "Doe",
    "dateOfBirth": "1990-05-15",
    "phoneNumber": "+919876543210",
    "city": "Bengaluru",
    "state": "Karnataka",
    "country": "India"
  }
  ```

---

### 4.5. Submit Account Opening Request
- **Method**: `POST`
- **Path**: `/api/v1/users/account-opening`
- **Headers**: `Authorization: Bearer <JWT>`
- **Request Body**:
  ```json
  {
    "requestId": "REQ-2026-001",
    "customerId": "C1001",
    "accountTypes": ["SAVINGS"]
  }
  ```
- **Response (`201 Created`)**:
  ```json
  {
    "requestId": "REQ-2026-001",
    "customerId": "C1001",
    "status": "PENDING",
    "createdAt": "2026-09-18T10:15:00"
  }
  ```

---

### 4.6. Admin: Approve Account Opening Request
- **Method**: `POST`
- **Path**: `/api/v1/users/admin/account-opening/{requestId}/approve`
- **Headers**: `Authorization: Bearer <ADMIN_JWT>`
- **Request Body**:
  ```json
  {
    "adminId": "ADMIN-01",
    "reason": "KYC verification verified successfully"
  }
  ```
- **Response (`200 OK`)**:
  ```json
  {
    "requestId": "REQ-2026-001",
    "customerId": "C1001",
    "status": "APPROVED",
    "reviewedBy": "ADMIN-01"
  }
  ```

---

## 5. Account & Ledger Service (`account-service`)

Responsible for bank accounts, current balances, double-entry immutable ledgers, 4-digit PIN security, and debits/credits.

### Endpoint Summary
| Method | Endpoint | Access | Summary |
| :--- | :--- | :---: | :--- |
| `POST` | `/api/v1/accounts` | Protected | Create a new bank account |
| `GET` | `/api/v1/accounts/{id}` | Protected | Get account details by Account ID |
| `GET` | `/api/v1/accounts?customerId={id}` | Protected | List accounts for a customer |
| `PATCH`| `/api/v1/accounts/{id}/status` | Protected | Change account status (ACTIVE, FROZEN, etc.) |
| `PATCH`| `/api/v1/accounts/{id}/type` | Protected | Change account type (SAVINGS, CHECKING) |
| `GET` | `/api/v1/accounts/{id}/balance` | Protected | Get current available & ledger balance |
| `POST` | `/api/v1/accounts/{id}/credits` | Internal/Auth | Post manual credit posting to ledger |
| `POST` | `/api/v1/accounts/{id}/debits` | Internal/Auth | Post manual debit posting to ledger |
| `POST` | `/api/v1/accounts/{id}/credit` | OpenFeign/Auth | Standardized balance credit operation |
| `POST` | `/api/v1/accounts/{id}/debit` | OpenFeign/Auth | Standardized balance debit operation |
| `GET` | `/api/v1/accounts/{id}/ledger` | Protected | List historical double-entry ledger entries |
| `PUT` | `/api/v1/accounts/{id}/pin` | Protected | Set or change 4-digit account PIN |
| `POST` | `/api/v1/accounts/{id}/pin/verify` | Protected | Verify 4-digit account PIN |

---

### 5.1. Create Bank Account
- **Method**: `POST`
- **Path**: `/api/v1/accounts`
- **Headers**: `Authorization: Bearer <JWT>`
- **Request Body**:
  ```json
  {
    "customerId": "C1001",
    "accountType": "SAVINGS",
    "currencyCode": "INR"
  }
  ```
  *(Account types: `SAVINGS`, `CHECKING`, `MONEY_MARKET`)*
- **Response (`201 Created`)**:
  ```json
  {
    "id": 1,
    "accountNumber": "ACC100000001",
    "customerId": "C1001",
    "accountType": "SAVINGS",
    "status": "ACTIVE",
    "currencyCode": "INR",
    "createdAt": "2026-09-18T10:30:00"
  }
  ```

---

### 5.2. Get Balance
- **Method**: `GET`
- **Path**: `/api/v1/accounts/{id}/balance`
- **Headers**: `Authorization: Bearer <JWT>`
- **Response (`200 OK`)**:
  ```json
  {
    "accountId": 1,
    "availableBalance": 50000.00,
    "ledgerBalance": 50000.00,
    "currencyCode": "INR",
    "lastUpdated": "2026-09-18T10:35:00"
  }
  ```

---

### 5.3. Standardized Debit Operation (OpenFeign Target)
- **Method**: `POST`
- **Path**: `/api/v1/accounts/{id}/debit`
- **Headers**: `Authorization: Bearer <JWT>`
- **Request Body**:
  ```json
  {
    "amount": 2500.00,
    "transactionReference": "TXN-2026-89410",
    "description": "Transfer to Acc 2",
    "initiatedBy": "CUSTOMER"
  }
  ```
- **Response (`200 OK`)**:
  ```json
  {
    "success": true,
    "message": "Account debited successfully"
  }
  ```

---

### 5.4. Standardized Credit Operation (OpenFeign Target)
- **Method**: `POST`
- **Path**: `/api/v1/accounts/{id}/credit`
- **Headers**: `Authorization: Bearer <JWT>`
- **Request Body**:
  ```json
  {
    "amount": 2500.00,
    "transactionReference": "TXN-2026-89410",
    "description": "Transfer from Acc 1",
    "initiatedBy": "CUSTOMER"
  }
  ```
- **Response (`200 OK`)**:
  ```json
  {
    "success": true,
    "message": "Account credited successfully"
  }
  ```

---

### 5.5. Set 4-digit Account PIN
- **Method**: `PUT`
- **Path**: `/api/v1/accounts/{id}/pin`
- **Headers**: `Authorization: Bearer <JWT>`
- **Request Body**:
  ```json
  {
    "pin": "1234"
  }
  ```
- **Response**: `204 No Content`

---

### 5.6. Verify 4-digit Account PIN
- **Method**: `POST`
- **Path**: `/api/v1/accounts/{id}/pin/verify`
- **Headers**: `Authorization: Bearer <JWT>`
- **Request Body**:
  ```json
  {
    "pin": "1234"
  }
  ```
- **Response (`200 OK`)**:
  ```json
  {
    "valid": true
  }
  ```

---

### 5.7. Get Account Ledger
- **Method**: `GET`
- **Path**: `/api/v1/accounts/{id}/ledger`
- **Headers**: `Authorization: Bearer <JWT>`
- **Response (`200 OK`)**:
  ```json
  [
    {
      "ledgerId": 1,
      "accountId": 1,
      "entryType": "CREDIT",
      "amount": 50000.00,
      "transactionReference": "INIT-DEPOSIT",
      "createdAt": "2026-09-18T10:30:00"
    }
  ]
  ```

---

## 6. Transaction & Statement Service (`transaction-service`)

Executes fund transfers (immediate or scheduled), enforces idempotency, validates source and destination accounts via OpenFeign, and generates statements.

### Endpoint Summary
| Method | Endpoint | Access | Summary |
| :--- | :--- | :---: | :--- |
| `POST` | `/api/v1/transactions/transfers` | Protected | Execute immediate or scheduled fund transfer |
| `POST` | `/api/v1/statements` | Protected | Request account statement generation |

---

### 6.1. Execute Fund Transfer
- **Method**: `POST`
- **Path**: `/api/v1/transactions/transfers` *(or `/api/v1/transactions/transfer`)*
- **Headers**:
  ```http
  Authorization: Bearer <JWT_ACCESS_TOKEN>
  X-Customer-Id: C1001
  X-Initiated-By: CUSTOMER
  X-User-Email: customer@example.com
  Idempotency-Key: IDEMP-KEY-98765
  Content-Type: application/json
  ```
- **Request Body**:
  ```json
  {
    "sourceAccountId": 1,
    "destinationAccountId": 2,
    "amount": 1500.00,
    "currency": "INR",
    "description": "Monthly rent split",
    "transferType": "IMMEDIATE",
    "transferMode": "INTERNAL",
    "scheduledAt": null
  }
  ```
- **Response (`200 OK`)**:
  ```json
  {
    "transactionId": 101,
    "transactionReference": "TXN-A1B2C3D4E5",
    "sourceAccountId": 1,
    "destinationAccountId": 2,
    "amount": 1500.00,
    "currency": "INR",
    "status": "COMPLETED",
    "failureReason": null,
    "createdAt": "2026-09-18T11:00:00"
  }
  ```
- **Lifecycle Executed**:
  1. Checks idempotency cache in `idempotency_records`.
  2. Calls `Account-Service` via OpenFeign to debit Account 1.
  3. Calls `Account-Service` via OpenFeign to credit Account 2.
  4. Emits `TRANSFER` event to Kafka topic `transaction-events` with `recipientEmail: customer@example.com`.
  5. `Notification-Service` delivers an alert email to the recipient.

---

### 6.2. Request Account Statement
- **Method**: `POST`
- **Path**: `/api/v1/statements` *(or `/api/v1/transactions/statements`)*
- **Headers**:
  ```http
  Authorization: Bearer <JWT>
  X-Customer-Id: C1001
  X-Initiated-By: CUSTOMER
  Content-Type: application/json
  ```
- **Request Body**:
  ```json
  {
    "accountId": 1,
    "requestType": "MONTHLY_STATEMENT",
    "fromDate": "2026-08-01T00:00:00",
    "toDate": "2026-08-31T23:59:59"
  }
  ```
- **Response (`200 OK`)**:
  ```json
  {
    "statementId": 1,
    "accountId": 1,
    "status": "GENERATED",
    "downloadUrl": "/api/v1/statements/1/download"
  }
  ```

---

## 7. Notification & Audit Service (`notification-service`)

Manages email dispatch via Google SMTP, stores audit logs, and controls access to sensitive audit trails.

### Endpoint Summary
| Method | Endpoint | Access | Summary |
| :--- | :--- | :---: | :--- |
| `POST` | `/api/v1/notifications` | Protected | Create manual notification record |
| `POST` | `/api/v1/notifications/{id}/send` | Protected | Trigger instant email dispatch via SMTP |
| `GET` | `/api/v1/notifications/{id}` | Protected | Get notification & delivery audit status |
| `GET` | `/api/v1/notifications/customer/{customerId}` | Protected | List all notifications for customer |
| `POST` | `/api/v1/audit/access/request` | Admin | Request audit log access clearance |
| `POST` | `/api/v1/audit/access/{requestId}/approve` | Admin | Approve audit clearance & issue token |
| `GET` | `/health` | Public | Service health verification |

---

### 7.1. Create Notification
- **Method**: `POST`
- **Path**: `/api/v1/notifications`
- **Headers**: `Authorization: Bearer <JWT>`
- **Request Body**:
  ```json
  {
    "eventId": "EVT-MANUAL-001",
    "eventType": "SECURITY_ALERT",
    "customerId": "C1001",
    "recipientEmail": "customer@example.com",
    "subject": "Security Alert: Login from new device",
    "messageBody": "Dear Customer, A login was detected from IP 192.168.1.1."
  }
  ```
- **Response (`200 OK`)**:
  ```json
  {
    "notificationId": 5,
    "eventId": "EVT-MANUAL-001",
    "eventType": "SECURITY_ALERT",
    "customerId": "C1001",
    "recipientEmail": "customer@example.com",
    "subject": "Security Alert: Login from new device",
    "status": "PENDING",
    "createdAt": "2026-09-18T11:15:00"
  }
  ```

---

### 7.2. Send Notification via SMTP
- **Method**: `POST`
- **Path**: `/api/v1/notifications/{id}/send`
- **Headers**: `Authorization: Bearer <JWT>`
- **Response**: `204 No Content`
- **Result**: JavaMailSender connects to `smtp.gmail.com:587` and delivers email.

---

### 7.3. Get Notification & Delivery Status
- **Method**: `GET`
- **Path**: `/api/v1/notifications/{id}`
- **Headers**: `Authorization: Bearer <JWT>`
- **Response (`200 OK`)**:
  ```json
  {
    "notificationId": 5,
    "status": "SENT",
    "sentAt": "2026-09-18T11:15:05",
    "deliveries": [
      {
        "deliveryId": 5,
        "attemptNumber": 1,
        "smtpProvider": "GOOGLE_SMTP",
        "deliveryStatus": "SUCCESS",
        "attemptedAt": "2026-09-18T11:15:04",
        "deliveredAt": "2026-09-18T11:15:05"
      }
    ]
  }
  ```

---

### 7.4. Request Audit Log Access
- **Method**: `POST`
- **Path**: `/api/v1/audit/access/request`
- **Headers**: `Authorization: Bearer <ADMIN_JWT>`
- **Request Body**:
  ```json
  {
    "adminId": "ADMIN-01",
    "reason": "Investigating unauthorized access incident INC-990"
  }
  ```
- **Response (`200 OK`)**:
  ```json
  {
    "requestId": 1,
    "adminId": "ADMIN-01",
    "status": "PENDING"
  }
  ```

---

### 7.5. Approve Audit Access
- **Method**: `POST`
- **Path**: `/api/v1/audit/access/{requestId}/approve`
- **Headers**: `Authorization: Bearer <ADMIN_JWT>`
- **Request Body**:
  ```json
  {
    "approvedBy": "SUPER_ADMIN"
  }
  ```
- **Response (`200 OK`)**:
  ```json
  {
    "token": "audit-access-token-9b12e..."
  }
  ```

---

## 8. Edge Gateway & Service Discovery Endpoints

### Eureka Discovery Server (`http://localhost:8761`)
- **Dashboard**: `GET http://localhost:8761/`
- **Registered Applications (XML/JSON)**:
  ```bash
  curl -H "Accept: application/json" http://localhost:8761/eureka/apps
  ```

### API Gateway Actuator (`http://localhost:8080`)
- **Gateway Health**: `GET http://localhost:8080/actuator/health`
- **Active Gateway Routes**: `GET http://localhost:8080/actuator/gateway/routes`

---

## 9. Inter-Service Communication (OpenFeign & Kafka)

### Synchronous OpenFeign Calls
| Calling Service | Target Service | Path | Purpose |
| :--- | :--- | :--- | :--- |
| `auth-service` | `user-service` | `POST /api/v1/customers` | Initialize customer shell upon registration |
| `transaction-service` | `account-service` | `POST /api/v1/accounts/{id}/debit` | Deduct funds from source account |
| `transaction-service` | `account-service` | `POST /api/v1/accounts/{id}/credit` | Deposit funds to destination account |
| `transaction-service` | `account-service` | `GET /api/v1/accounts/{id}` | Validate account status & currency |
| `transaction-service` | `user-service` | `GET /api/v1/customers/{customerId}` | Validate customer KYC status |

### Asynchronous Kafka Topics
| Topic Name | Producer | Consumer | Payload Event Types |
| :--- | :--- | :--- | :--- |
| `auth-events` | `auth-service` | `notification-service` | `OTP_ISSUED`, `USER_REGISTERED`, `LOGIN_SUCCESS`, `PASSWORD_RESET_REQUESTED` |
| `transaction-events`| `transaction-service`| `notification-service` | `TRANSFER`, `DEBIT`, `CREDIT`, `TRANSACTION_FAILED` |
