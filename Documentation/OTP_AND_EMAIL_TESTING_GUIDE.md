# NetBanking Platform: OTP & Email (SMTP) Testing Guide

Comprehensive manual testing and API verification guide for **One-Time Passwords (OTP)**, **Multi-Factor Authentication (MFA)**, and **SMTP Email Notifications** across the NetBanking microservices ecosystem.

---

## 1. Architecture Overview

Email notifications and OTP workflows are handled through a combination of synchronous REST APIs and event-driven Apache Kafka messaging:

```
                          ┌───────────────────────────┐
                          │    Spring Cloud Gateway   │
                          │        (Port 8080)        │
                          └─────────────┬─────────────┘
                                        │
                 ┌──────────────────────┴──────────────────────┐
                 │                                             │
                 ▼                                             ▼
        ┌─────────────────┐                           ┌─────────────────┐
        │   Auth-Service  │                           │  Notification-  │
        │   (Port 8081)   │                           │     Service     │
        └────────┬────────┘                           │   (Port 8085)   │
                 │                                    └────────▲────────┘
                 │ Kafka: "auth-events"                        │
                 │ (Event: OTP_ISSUED)                         │
                 └─────────────────────────────────────────────┘
                                                               │
                                                               ▼
                                                      ┌─────────────────┐
                                                      │   Google SMTP   │
                                                      │  (smtp.gmail)   │
                                                      └────────┬────────┘
                                                               │
                                                               ▼
                                                      ┌─────────────────┐
                                                      │ User Mail Inbox │
                                                      └─────────────────┘
```

### Communication Channels
1. **Direct Notification API (`Notification-Service`)**: Synchronous REST API allowing on-demand creation and instant delivery of custom emails via Google SMTP.
2. **Event-Driven OTP Workflow (`Auth-Service` ➔ Kafka ➔ `Notification-Service`)**:
   - `Auth-Service` generates a cryptographic 6-digit OTP, stores a BCrypt hash in `AUTH_SCHEMA.EMAIL_OTPS`, and emits an `OTP_ISSUED` event to Kafka topic `auth-events`.
   - `Notification-Service` (`AuthNotificationConsumer`) consumes the event, persists a notification record in `NOTIFICATION_SCHEMA`, and dispatches the email via SMTP.
3. **Transaction Alerts (`Transaction-Service` ➔ Kafka ➔ `Notification-Service`)**:
   - Fund transfers publish events to `transaction-events` with recipient email extracted from the `X-User-Email` header.
   - `Notification-Service` (`TransactionNotificationConsumer`) sends debit/credit alerts automatically.

---

## 2. Infrastructure & Port Mapping

| Service | Port (Direct) | Port (Gateway) | Role in Email / OTP |
| :--- | :---: | :---: | :--- |
| **API Gateway** | `8080` | `8080` | Single entry point for all requests |
| **Auth Service** | `8081` | `8080/api/v1/auth/**` | Generates OTPs, handles verification & MFA |
| **Notification Service** | `8085` | `8080/api/v1/notifications/**` | Sends emails via JavaMailSender / Google SMTP |
| **Transaction Service** | `8084` | `8080/api/v1/transactions/**` | Emits transaction events with recipient email |
| **Apache Kafka** | `9092` | N/A | Transports `auth-events` and `transaction-events` |
| **Oracle Database** | `1521` | N/A | Stores OTP hashes and delivery audit records |

---

## 3. Workflow 1: Direct SMTP Email Testing (Notification Service)

Use these endpoints to directly test your Google SMTP configuration and send emails immediately.

### Step 1.1: Create a Notification Record

- **Method**: `POST`
- **Gateway URL**: `http://localhost:8080/api/v1/notifications`
- **Direct URL**: `http://localhost:8085/api/v1/notifications`
- **Headers**:
  ```http
  Content-Type: application/json
  ```
- **Request Body**:
  ```json
  {
    "eventId": "EVT-MANUAL-001",
    "eventType": "OTP_VERIFICATION",
    "customerId": "CUST1001",
    "recipientEmail": "deekshi.m.2024@gmail.com",
    "subject": "NetBanking Security Alert: One-Time Password",
    "messageBody": "Dear Customer,\n\nYour test verification code is: 849201.\n\nThis code expires in 5 minutes.\n\nWarm regards,\nNetBanking Security Team"
  }
  ```

- **Expected Response (`200 OK`)**:
  ```json
  {
    "notificationId": 3,
    "eventId": "EVT-MANUAL-001",
    "eventType": "OTP_VERIFICATION",
    "customerId": "CUST1001",
    "recipientEmail": "deekshi.m.2024@gmail.com",
    "subject": "NetBanking Security Alert: One-Time Password",
    "messageBody": "Dear Customer,\n\nYour test verification code is: 849201...",
    "status": "PENDING",
    "createdAt": "2026-09-18T11:20:00"
  }
  ```
> **Note**: Retain the `notificationId` returned (e.g., `3`) for Step 1.2.

---

### Step 1.2: Trigger Instant SMTP Email Dispatch

- **Method**: `POST`
- **Gateway URL**: `http://localhost:8080/api/v1/notifications/{id}/send`
- **Direct URL**: `http://localhost:8085/api/v1/notifications/{id}/send`
  - *(Example: `http://localhost:8080/api/v1/notifications/3/send`)*
- **Headers**: None
- **Request Body**: None
- **Expected Response**: `204 No Content`
- **Result**: Notification service connects to `smtp.gmail.com:587`, performs STARTTLS authentication, and dispatches the email.

---

### Step 1.3: Inspect Notification & Delivery History

- **Method**: `GET`
- **Gateway URL**: `http://localhost:8080/api/v1/notifications/3`
- **Direct URL**: `http://localhost:8085/api/v1/notifications/3`
- **Expected Response (`200 OK`)**:
  ```json
  {
    "notificationId": 3,
    "eventId": "EVT-MANUAL-001",
    "eventType": "OTP_VERIFICATION",
    "customerId": "CUST1001",
    "recipientEmail": "deekshi.m.2024@gmail.com",
    "subject": "NetBanking Security Alert: One-Time Password",
    "status": "SENT",
    "sentAt": "2026-09-18T11:20:05",
    "failureReason": null,
    "deliveries": [
      {
        "deliveryId": 3,
        "attemptNumber": 1,
        "smtpProvider": "GOOGLE_SMTP",
        "deliveryStatus": "SUCCESS",
        "attemptedAt": "2026-09-18T11:20:04",
        "deliveredAt": "2026-09-18T11:20:05",
        "errorMessage": null
      }
    ]
  }
  ```

---

## 4. Workflow 2: User Registration & Email Verification OTP

This tests automatic event propagation: user registration triggers OTP generation, sends an event to Kafka, and dispatches an email to the inbox.

### Step 2.1: Register a New Identity

- **Method**: `POST`
- **Gateway URL**: `http://localhost:8080/api/v1/auth/register`
- **Direct URL**: `http://localhost:8081/api/v1/auth/register`
- **Headers**:
  ```http
  Content-Type: application/json
  ```
- **Request Body**:
  ```json
  {
    "email": "deekshi.m.2024@gmail.com",
    "password": "SecurePassword123!"
  }
  ```
- **Expected Response (`200 OK`)**:
  ```json
  {
    "message": "Registration successful. An email verification OTP has been issued."
  }
  ```
- **What Happens**:
  1. Record created in `AUTH_SCHEMA.USERS` with `EMAIL_VERIFIED = 'N'` and `MFA_ENABLED = 'Y'`.
  2. 6-digit random code generated and hashed into `AUTH_SCHEMA.EMAIL_OTPS` with purpose `EMAIL_VERIFICATION`.
  3. `Auth-Service` publishes event `OTP_ISSUED` to Kafka topic `auth-events`.
  4. `Notification-Service` receives event and sends email with subject `"NetBanking Security Verification - OTP Code"`.

---

### Step 2.2: Verify Email Registration OTP

- **Method**: `POST`
- **Gateway URL**: `http://localhost:8080/api/v1/auth/verify-otp`
- **Direct URL**: `http://localhost:8081/api/v1/auth/verify-otp`
- **Headers**:
  ```http
  Content-Type: application/json
  ```
- **Request Body**:
  ```json
  {
    "email": "deekshi.m.2024@gmail.com",
    "otp": "492015",
    "purpose": "EMAIL_VERIFICATION"
  }
  ```
  *(Replace `492015` with the 6-digit OTP from your email)*
- **Expected Response (`200 OK`)**:
  ```json
  {
    "message": "OTP verified successfully"
  }
  ```
- **Result**: `AUTH_SCHEMA.USERS.EMAIL_VERIFIED` is updated to `'Y'`.

---

## 5. Workflow 3: Multi-Factor Authentication (MFA / 2FA) Login OTP

Because `MFA_ENABLED = 'Y'` is set on registration, logging in requires two steps.

### Step 3.1: Initiate Login (Trigger 2FA OTP)

- **Method**: `POST`
- **Gateway URL**: `http://localhost:8080/api/v1/auth/login`
- **Direct URL**: `http://localhost:8081/api/v1/auth/login`
- **Headers**:
  ```http
  Content-Type: application/json
  ```
- **Request Body**:
  ```json
  {
    "email": "deekshi.m.2024@gmail.com",
    "password": "SecurePassword123!"
  }
  ```
- **Expected Response (`200 OK`)**:
  ```json
  {
    "accessToken": null,
    "refreshToken": null,
    "tokenType": "Bearer",
    "expiresIn": 0,
    "customerId": "C1A2B3C4D5E6",
    "roles": [
      "CUSTOMER"
    ],
    "mfaRequired": true
  }
  ```
- **What Happens**: `mfaRequired: true` indicates that password validation succeeded and a login OTP has been sent to your email.

---

### Step 3.2: Complete Login with 2FA OTP

- **Method**: `POST`
- **Gateway URL**: `http://localhost:8080/api/v1/auth/login`
- **Direct URL**: `http://localhost:8081/api/v1/auth/login`
- **Headers**:
  ```http
  Content-Type: application/json
  ```
- **Request Body**:
  ```json
  {
    "email": "deekshi.m.2024@gmail.com",
    "password": "SecurePassword123!",
    "otp": "581934"
  }
  ```
  *(Replace `581934` with the login OTP received in email)*
- **Expected Response (`200 OK`)**:
  ```json
  {
    "accessToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
    "refreshToken": "7c88b776-96a9-4674-8b2b-426c1fe64e3c...",
    "tokenType": "Bearer",
    "expiresIn": 3600,
    "customerId": "C1A2B3C4D5E6",
    "roles": [
      "CUSTOMER"
    ],
    "mfaRequired": false
  }
  ```

---

## 6. Workflow 4: Password Recovery & Reset Flow

### Step 4.1: Request Password Reset Instructions

- **Method**: `POST`
- **Gateway URL**: `http://localhost:8080/api/v1/auth/forgot-password`
- **Direct URL**: `http://localhost:8081/api/v1/auth/forgot-password`
- **Headers**:
  ```http
  Content-Type: application/json
  ```
- **Request Body**:
  ```json
  {
    "email": "deekshi.m.2024@gmail.com"
  }
  ```
- **Expected Response (`200 OK`)**:
  ```json
  {
    "message": "If the account exists, password reset instructions have been issued."
  }
  ```

---

### Step 4.2: Reset Password with Token

- **Method**: `POST`
- **Gateway URL**: `http://localhost:8080/api/v1/auth/reset-password`
- **Direct URL**: `http://localhost:8081/api/v1/auth/reset-password`
- **Headers**:
  ```http
  Content-Type: application/json
  ```
- **Request Body**:
  ```json
  {
    "token": "<RESET_TOKEN_FROM_EMAIL_OR_DB>",
    "newPassword": "NewSecurePassword456!"
  }
  ```
- **Expected Response (`200 OK`)**:
  ```json
  {
    "message": "Password reset successful"
  }
  ```

---

## 7. Workflow 5: Transaction Notification Emails

Whenever a fund transfer occurs, `Transaction-Service` publishes an event carrying the recipient's email address (sourced from the client's `X-User-Email` header).

### Trigger Fund Transfer:

- **Method**: `POST`
- **Gateway URL**: `http://localhost:8080/api/v1/transactions/transfers`
- **Headers**:
  ```http
  Authorization: Bearer <JWT_ACCESS_TOKEN>
  X-Customer-Id: C1A2B3C4D5E6
  X-User-Email: deekshi.m.2024@gmail.com
  Content-Type: application/json
  ```
- **Request Body**:
  ```json
  {
    "sourceAccountId": 1,
    "destinationAccountId": 2,
    "amount": 2500.00,
    "currency": "INR",
    "description": "Bill Payment",
    "pin": "1234"
  }
  ```
- **Result**: `Notification-Service` automatically receives the event via `transaction-events` Kafka topic and delivers an alert email:
  - **Subject**: `NetBanking Alert: Transaction COMPLETED (INR 2500.00)`
  - **Body**: Contains transaction reference, amount, and timestamp.

---

## 8. Quick Terminal cURL Commands

### Send Direct Test Email:
```bash
# 1. Create notification
curl -X POST "http://localhost:8080/api/v1/notifications" \
  -H "Content-Type: application/json" \
  -d '{
    "eventId": "EVT-TEST-001",
    "eventType": "OTP_VERIFICATION",
    "customerId": "CUST1001",
    "recipientEmail": "deekshi.m.2024@gmail.com",
    "subject": "NetBanking Manual Test OTP",
    "messageBody": "Your verification code is 654321."
  }'

# 2. Dispatch via SMTP (replace ID with returned notificationId)
curl -X POST "http://localhost:8080/api/v1/notifications/1/send"
```

### Verify OTP via cURL:
```bash
curl -X POST "http://localhost:8080/api/v1/auth/verify-otp" \
  -H "Content-Type: application/json" \
  -d '{
    "email": "deekshi.m.2024@gmail.com",
    "otp": "654321",
    "purpose": "EMAIL_VERIFICATION"
  }'
```

---

## 9. Database Audit Verification (Oracle Database)

To verify the state of notifications and OTPs directly in the database:

### Check Notification Delivery Records:
```sql
SELECT n.NOTIFICATION_ID, n.RECIPIENT_EMAIL, n.STATUS, n.SENT_AT,
       d.DELIVERY_ID, d.SMTP_PROVIDER, d.DELIVERY_STATUS, d.ATTEMPTED_AT
FROM NOTIFICATION_SCHEMA.NOTIFICATIONS n
LEFT JOIN NOTIFICATION_SCHEMA.NOTIFICATION_DELIVERY d 
       ON n.NOTIFICATION_ID = d.NOTIFICATION_ID
ORDER BY n.NOTIFICATION_ID DESC;
```

### Check Generated OTP Records:
```sql
SELECT o.OTP_ID, u.EMAIL, o.OTP_PURPOSE, o.EXPIRES_AT, o.ATTEMPT_COUNT, o.VERIFIED_AT
FROM AUTH_SCHEMA.EMAIL_OTPS o
JOIN AUTH_SCHEMA.USERS u ON o.USER_ID = u.USER_ID
ORDER BY o.CREATED_AT DESC;
```

---

## 10. Key Rules & Constraints

1. **OTP Lifespan**: OTPs automatically expire after **5 minutes** (`expiresAt: LocalDateTime.now().plusMinutes(5)`).
2. **Brute Force Protection**: Maximum of **5 incorrect attempts** allowed per OTP (`attemptCount >= 5` throws `OTP attempt limit exceeded`).
3. **Purpose Validation**: The `purpose` in `verify-otp` (`EMAIL_VERIFICATION`, `LOGIN`, `PASSWORD_RESET`) must match the purpose for which the OTP was issued.
4. **Google SMTP Requirements**:
   - Host: `smtp.gmail.com`
   - Port: `587`
   - Protocol: `STARTTLS`
   - Auth: Google **App Password** (16 characters, 2FA enabled on Google Account).
