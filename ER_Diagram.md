# NetBanking System - Entity Relationship (ER) Documentation

This document provides complete Entity Relationship (ER) diagrams and comprehensive data dictionaries for all 5 microservices in the NetBanking system:
1. [Auth Service (`AUTH_SCHEMA`)](#1-auth-service-auth_schema)
2. [User Service (`USER_SCHEMA`)](#2-user-service-user_schema)
3. [Account Service (`ACCOUNT_SCHEMA`)](#3-account-service-account_schema)
4. [Transaction Service (`TRANSACTION_SCHEMA`)](#4-transaction-service-transaction_schema)
5. [Notification & Audit Service (`NOTIFICATION_SCHEMA`)](#5-notification--audit-service-notification_schema)
6. [Cross-Service Logical Relationships](#6-cross-service-logical-relationships)

---

## 1. Auth Service (`AUTH_SCHEMA`)

### Responsibilities
Manages identity registration, credentials, login/logout, password reset, MFA/OTP, refresh tokens, session tracking, and user role assignments.

### Mermaid Diagram
```mermaid
erDiagram
    AUTH_USER ||--o{ USER_ROLE : "has roles"
    AUTH_USER ||--o{ EMAIL_OTP : "issues OTPs"
    AUTH_USER ||--o{ REFRESH_TOKEN : "owns refresh tokens"
    AUTH_USER ||--o{ PASSWORD_RESET_TOKEN : "requests password resets"
    AUTH_USER ||--o{ LOGIN_HISTORY : "records login events"

    AUTH_USER {
        NUMBER user_id PK "Identity auto-increment"
        VARCHAR2(50) customer_id UK "Unique reference to Customer"
        VARCHAR2(255) email UK "Unique login email"
        VARCHAR2(255) password_hash "Hashed password"
        VARCHAR2(20) account_status "ACTIVE, LOCKED, DISABLED, BLOCKED, PENDING_APPROVAL"
        CHAR(1) email_verified "Y or N"
        CHAR(1) mfa_enabled "Y or N"
        NUMBER failed_login_attempts "Default 0"
        TIMESTAMP locked_until "Lock expiration timestamp"
        TIMESTAMP last_login_at "Last authenticated timestamp"
        TIMESTAMP created_at "Creation timestamp"
        TIMESTAMP updated_at "Update timestamp"
    }

    USER_ROLE {
        NUMBER user_role_id PK "Identity auto-increment"
        NUMBER user_id FK "References AUTH_USER"
        VARCHAR2(30) role_name "CUSTOMER or ADMIN"
        TIMESTAMP created_at "Creation timestamp"
    }

    EMAIL_OTP {
        NUMBER otp_id PK "Identity auto-increment"
        NUMBER user_id FK "References AUTH_USER"
        VARCHAR2(255) otp_hash "Hashed one-time password"
        VARCHAR2(30) otp_purpose "LOGIN, PASSWORD_RESET, EMAIL_VERIFICATION"
        TIMESTAMP expires_at "Expiry timestamp"
        TIMESTAMP verified_at "Verification timestamp"
        NUMBER attempt_count "Attempt counter >= 0"
        TIMESTAMP created_at "Creation timestamp"
    }

    REFRESH_TOKEN {
        NUMBER refresh_token_id PK "Identity auto-increment"
        NUMBER user_id FK "References AUTH_USER"
        VARCHAR2(500) token_hash UK "Hashed refresh token"
        TIMESTAMP expires_at "Expiry timestamp"
        TIMESTAMP revoked_at "Revocation timestamp"
        TIMESTAMP created_at "Creation timestamp"
    }

    PASSWORD_RESET_TOKEN {
        NUMBER reset_token_id PK "Identity auto-increment"
        NUMBER user_id FK "References AUTH_USER"
        VARCHAR2(500) token_hash UK "Hashed password reset token"
        TIMESTAMP expires_at "Expiry timestamp"
        TIMESTAMP used_at "Consumed timestamp"
        TIMESTAMP created_at "Creation timestamp"
    }

    LOGIN_HISTORY {
        NUMBER login_history_id PK "Identity auto-increment"
        NUMBER user_id FK "References AUTH_USER"
        VARCHAR2(30) login_status "SUCCESS, FAILED, LOGOUT, LOCKED"
        VARCHAR2(45) ip_address "Client IP address"
        VARCHAR2(500) user_agent "Client user agent"
        TIMESTAMP login_at "Attempt timestamp"
    }
```

---

## 2. User Service (`USER_SCHEMA`)

### Responsibilities
Maintains customer profile records, addresses, contact details, account opening requests, and the admin customer onboarding approval lifecycle.

### Mermaid Diagram
```mermaid
erDiagram
    CUSTOMER ||--|| CUSTOMER_PROFILE : "has profile details"
    CUSTOMER ||--o{ ACCOUNT_OPENING_REQUEST : "submits requests"
    ACCOUNT_OPENING_REQUEST ||--|{ ACCOUNT_OPENING_REQUEST_TYPE : "requests account types"

    CUSTOMER {
        VARCHAR2(50) customer_id PK "Canonical Customer Identifier"
        VARCHAR2(30) customer_status "PENDING_APPROVAL, ACTIVE, INACTIVE, BLOCKED, REJECTED"
        TIMESTAMP created_at "Record creation timestamp"
        TIMESTAMP updated_at "Last update timestamp"
    }

    CUSTOMER_PROFILE {
        VARCHAR2(50) customer_id PK,FK "References CUSTOMER (1:1)"
        VARCHAR2(100) first_name "Customer first name"
        VARCHAR2(100) last_name "Customer surname"
        DATE date_of_birth "Birthdate"
        VARCHAR2(20) phone_number "Contact number"
        VARCHAR2(255) address_line1 "Address line 1"
        VARCHAR2(255) address_line2 "Address line 2"
        VARCHAR2(100) city "City"
        VARCHAR2(100) state "State / Province"
        VARCHAR2(20) postal_code "Postal code"
        VARCHAR2(100) country "Country"
        TIMESTAMP created_at "Creation timestamp"
        TIMESTAMP updated_at "Update timestamp"
    }

    ACCOUNT_OPENING_REQUEST {
        VARCHAR2(30) request_id PK "Unique request reference"
        VARCHAR2(50) customer_id FK "References CUSTOMER"
        VARCHAR2(20) request_status "PENDING, APPROVED, REJECTED"
        TIMESTAMP submitted_at "Submission timestamp"
        VARCHAR2(50) reviewed_by "Admin identity"
        TIMESTAMP reviewed_at "Review decision timestamp"
        VARCHAR2(500) rejection_reason "Explanation if rejected"
        TIMESTAMP created_at "Creation timestamp"
        TIMESTAMP updated_at "Update timestamp"
    }

    ACCOUNT_OPENING_REQUEST_TYPE {
        NUMBER request_type_id PK "Identity auto-increment"
        VARCHAR2(30) request_id FK "References ACCOUNT_OPENING_REQUEST"
        VARCHAR2(20) account_type "SAVINGS or CURRENT"
    }
```

---

## 3. Account Service (`ACCOUNT_SCHEMA`)

### Responsibilities
Source of truth for bank account state, ledger accounting (double-entry debit/credit), current and available balances, 4-digit hashed PIN security, and account type transitions.

### Mermaid Diagram
```mermaid
erDiagram
    ACCOUNT ||--|| ACCOUNT_BALANCE : "has balance"
    ACCOUNT ||--|| ACCOUNT_PIN : "secured by PIN"
    ACCOUNT ||--o{ ACCOUNT_LEDGER : "records ledger entries"
    ACCOUNT ||--o{ ACCOUNT_TYPE_HISTORY : "tracks type changes"

    ACCOUNT {
        NUMBER account_id PK "Identity auto-increment"
        VARCHAR2(50) customer_id "Logical reference to Customer"
        VARCHAR2(20) account_number UK "Unique bank account number"
        VARCHAR2(20) account_type "SAVINGS or CURRENT"
        VARCHAR2(30) account_status "PENDING_APPROVAL, ACTIVE, FROZEN, BLOCKED, CLOSED, REJECTED"
        VARCHAR2(3) currency_code "Default 'INR'"
        TIMESTAMP created_at "Creation timestamp"
        TIMESTAMP updated_at "Update timestamp"
        TIMESTAMP approved_at "Admin approval timestamp"
        TIMESTAMP closed_at "Closure timestamp"
        VARCHAR2(500) closure_reason "Closure reason"
    }

    ACCOUNT_BALANCE {
        NUMBER account_id PK,FK "References ACCOUNT (1:1)"
        NUMBER(19_2) current_balance "Current balance >= 0"
        NUMBER(19_2) available_balance "Available balance <= Current balance"
        NUMBER(19_2) total_credit "Cumulative credits >= 0"
        NUMBER(19_2) total_debit "Cumulative debits >= 0"
        NUMBER version_no "Optimistic locking counter"
        TIMESTAMP created_at "Creation timestamp"
        TIMESTAMP updated_at "Update timestamp"
    }

    ACCOUNT_PIN {
        NUMBER account_id PK,FK "References ACCOUNT (1:1)"
        VARCHAR2(255) pin_hash "Hashed 4-digit PIN"
        TIMESTAMP pin_created_at "PIN creation timestamp"
        NUMBER failed_attempts "Consecutive failed PIN attempts"
        TIMESTAMP locked_until "PIN lockout expiration"
        TIMESTAMP updated_at "Update timestamp"
    }

    ACCOUNT_LEDGER {
        NUMBER ledger_id PK "Identity auto-increment"
        NUMBER account_id FK "References ACCOUNT"
        VARCHAR2(100) transaction_reference "Logical reference to Transaction"
        VARCHAR2(100) entry_reference UK "Unique ledger entry reference"
        VARCHAR2(10) entry_type "DEBIT or CREDIT"
        NUMBER(19_2) amount "Amount > 0"
        NUMBER(19_2) balance_before "Pre-transaction balance"
        NUMBER(19_2) balance_after "Post-transaction balance"
        NUMBER(19_2) available_balance_before "Pre-transaction available balance"
        NUMBER(19_2) available_balance_after "Post-transaction available balance"
        VARCHAR2(500) description "Ledger narrative"
        VARCHAR2(100) created_by "Initiator identifier"
        TIMESTAMP created_at "Immutable append timestamp"
    }

    ACCOUNT_TYPE_HISTORY {
        NUMBER history_id PK "Identity auto-increment"
        NUMBER account_id FK "References ACCOUNT"
        VARCHAR2(20) old_account_type "SAVINGS or CURRENT"
        VARCHAR2(20) new_account_type "SAVINGS or CURRENT"
        VARCHAR2(100) changed_by "Admin or Customer identity"
        VARCHAR2(500) reason "Reason for change"
        TIMESTAMP changed_at "Change timestamp"
    }
```

---

## 4. Transaction Service (`TRANSACTION_SCHEMA`)

### Responsibilities
Orchestrates fund transfers, self transfers, idempotency verification, immutable lifecycle status history, and account statement requests.

### Mermaid Diagram
```mermaid
erDiagram
    TRANSACTIONS ||--|| TRANSFER_DETAILS : "has transfer details"
    TRANSACTIONS ||--o{ TRANSACTION_STATUS_HISTORY : "records status transitions"
    TRANSACTIONS ||--o| IDEMPOTENCY_RECORDS : "linked idempotency key"

    TRANSACTIONS {
        NUMBER(19) transaction_id PK "Auto-sequence: SEQ_TRANSACTION_ID"
        VARCHAR2(100) transaction_reference UK "Unique transaction reference"
        VARCHAR2(30) transaction_type "FUND_TRANSFER, SELF_TRANSFER, CUSTOMER_TRANSFER"
        VARCHAR2(20) transaction_status "INITIATED, PROCESSING, SUCCESS, FAILED, REVERSED"
        NUMBER(19) source_account_id "Source account ID (Account Service)"
        NUMBER(19) destination_account_id "Destination account ID (Account Service)"
        VARCHAR2(50) customer_id "Customer initiating the transaction"
        VARCHAR2(50) initiated_by "Actor / Token identity"
        NUMBER(19_2) amount "Amount > 0"
        VARCHAR2(3) currency "Currency code (e.g. INR)"
        VARCHAR2(255) description "Transaction description"
        VARCHAR2(500) failure_reason "Failure reason if failed"
        TIMESTAMP initiated_at "Initiation timestamp"
        TIMESTAMP completed_at "Completion timestamp"
        TIMESTAMP created_at "Creation timestamp"
        TIMESTAMP updated_at "Update timestamp"
    }

    TRANSFER_DETAILS {
        NUMBER(19) transfer_id PK "Auto-sequence: SEQ_TRANSFER_ID"
        NUMBER(19) transaction_id UK,FK "References TRANSACTIONS (1:1)"
        VARCHAR2(30) transfer_type "INTERNAL, SELF, FUND_TRANSFER, CUSTOMER_TRANSFER"
        VARCHAR2(20) transfer_mode "IMMEDIATE or SCHEDULED"
        VARCHAR2(255) remarks "Transfer remarks"
        TIMESTAMP scheduled_at "Execution time if scheduled"
        TIMESTAMP created_at "Creation timestamp"
    }

    TRANSACTION_STATUS_HISTORY {
        NUMBER(19) history_id PK "Auto-sequence: SEQ_STATUS_HISTORY_ID"
        NUMBER(19) transaction_id FK "References TRANSACTIONS"
        VARCHAR2(20) previous_status "Previous transaction status"
        VARCHAR2(20) new_status "Updated transaction status"
        VARCHAR2(500) reason "Status change explanation"
        TIMESTAMP changed_at "Timestamp of change"
    }

    IDEMPOTENCY_RECORDS {
        NUMBER(19) idempotency_id PK "Auto-sequence: SEQ_IDEMPOTENCY_ID"
        VARCHAR2(100) idempotency_key UK "Client-supplied idempotency key"
        VARCHAR2(50) customer_id "Customer making request"
        NUMBER(19) transaction_id FK "References TRANSACTIONS (optional)"
        VARCHAR2(128) request_hash "SHA-256 hash of payload"
        VARCHAR2(20) status "PROCESSING, COMPLETED, FAILED"
        TIMESTAMP created_at "Creation timestamp"
        TIMESTAMP expires_at "Expiration timestamp"
    }

    STATEMENT_REQUESTS {
        NUMBER(19) request_id PK "Auto-sequence: SEQ_STATEMENT_REQUEST_ID"
        NUMBER(19) account_id "Target account ID"
        VARCHAR2(50) customer_id "Customer requesting statement"
        VARCHAR2(50) requested_by "Actor requesting statement"
        VARCHAR2(10) request_type "PDF or CSV"
        TIMESTAMP from_date "Statement start date"
        TIMESTAMP to_date "Statement end date"
        VARCHAR2(20) status "PROCESSING, COMPLETED, FAILED"
        VARCHAR2(255) file_name "Generated file name"
        VARCHAR2(500) file_url "Download / Storage URL"
        TIMESTAMP requested_at "Request timestamp"
        TIMESTAMP completed_at "Generation timestamp"
        VARCHAR2(500) failure_reason "Failure reason if failed"
    }
```

---

## 5. Notification & Audit Service (`NOTIFICATION_SCHEMA`)

### Responsibilities
Consumes Kafka events to send customer emails via Google SMTP, records immutable audit trails across all microservices, and manages time-bound admin audit access passkeys/tokens.

### Mermaid Diagram
```mermaid
erDiagram
    NOTIFICATIONS ||--o{ NOTIFICATION_DELIVERY : "tracks delivery attempts"
    AUDIT_ACCESS_REQUESTS ||--o{ AUDIT_ACCESS_TOKENS : "issues passkey tokens"

    NOTIFICATIONS {
        NUMBER(19) notification_id PK "Identity auto-increment"
        VARCHAR2(100) event_id UK "Kafka idempotency event ID"
        VARCHAR2(100) event_type "Domain event name"
        VARCHAR2(50) customer_id "Target customer reference"
        VARCHAR2(255) recipient_email "Target recipient email"
        VARCHAR2(255) subject "Email subject"
        CLOB message_body "Email body content"
        VARCHAR2(20) status "PENDING, SENT, FAILED"
        TIMESTAMP(6) created_at "Event receipt timestamp"
        TIMESTAMP(6) sent_at "Sent timestamp"
        VARCHAR2(1000) failure_reason "Failure reason"
    }

    NOTIFICATION_DELIVERY {
        NUMBER(19) delivery_id PK "Identity auto-increment"
        NUMBER(19) notification_id FK "References NOTIFICATIONS"
        NUMBER(5) attempt_number "Attempt count > 0"
        VARCHAR2(50) smtp_provider "GOOGLE_SMTP, MOCK_SMTP, DEV_SMTP, AWS_SES"
        VARCHAR2(20) delivery_status "SUCCESS or FAILED"
        TIMESTAMP(6) attempted_at "Attempt timestamp"
        TIMESTAMP(6) delivered_at "Delivery timestamp"
        VARCHAR2(100) error_code "SMTP error code"
        VARCHAR2(1000) error_message "SMTP error message"
        VARCHAR2(1000) response_message "Server response message"
        VARCHAR2(255) message_id "Provider message ID"
    }

    AUDIT_LOGS {
        NUMBER(19) audit_id PK "Identity auto-increment"
        VARCHAR2(100) event_id UK "Kafka event identifier"
        VARCHAR2(100) event_type "Event type"
        VARCHAR2(50) actor_id "User / Admin identifier"
        VARCHAR2(20) actor_role "CUSTOMER or ADMIN"
        VARCHAR2(50) customer_id "Affected customer identifier"
        VARCHAR2(50) resource_type "ACCOUNT, TRANSACTION, CUSTOMER, AUTH"
        VARCHAR2(100) resource_id "Referenced resource ID"
        VARCHAR2(100) action "Action executed"
        VARCHAR2(1000) description "Audit narrative"
        VARCHAR2(45) ip_address "Origin IP address"
        VARCHAR2(500) user_agent "Client user agent"
        TIMESTAMP(6) event_timestamp "Event timestamp"
    }

    AUDIT_ACCESS_REQUESTS {
        NUMBER(19) request_id PK "Identity auto-increment"
        VARCHAR2(50) admin_id "Admin requesting audit access"
        VARCHAR2(20) status "PENDING, APPROVED, REJECTED, EXPIRED"
        TIMESTAMP(6) requested_at "Request timestamp"
        VARCHAR2(50) approved_by "Audit team officer"
        TIMESTAMP(6) approved_at "Approval timestamp"
        TIMESTAMP(6) expires_at "Expiration timestamp (e.g. 5 min)"
        VARCHAR2(500) reason "Access justification"
    }

    AUDIT_ACCESS_TOKENS {
        NUMBER(19) token_id PK "Identity auto-increment"
        NUMBER(19) request_id FK "References AUDIT_ACCESS_REQUESTS"
        VARCHAR2(255) token_hash UK "Hashed time-bound passkey"
        TIMESTAMP(6) issued_at "Issue timestamp"
        TIMESTAMP(6) expires_at "Expiry timestamp"
        TIMESTAMP(6) used_at "Consumed timestamp"
        VARCHAR2(20) status "ACTIVE, USED, EXPIRED, REVOKED"
    }
```

---

## 6. Cross-Service Logical Relationships

In accordance with the microservice database-per-service pattern, **no physical foreign keys exist between schemas**. Cross-service references are maintained logically through canonical identifiers communicated via OpenFeign REST APIs and Kafka events.

### Cross-Service Entity Map
```mermaid
flowchart LR
    subgraph AUTH_SCHEMA["Auth Service"]
        AU["AUTH_USER<br/>PK: USER_ID<br/>UK: CUSTOMER_ID"]
    end

    subgraph USER_SCHEMA["User Service"]
        CU["CUSTOMER<br/>PK: CUSTOMER_ID"]
        AOR["ACCOUNT_OPENING_REQUEST<br/>FK: CUSTOMER_ID"]
    end

    subgraph ACCOUNT_SCHEMA["Account Service"]
        AC["ACCOUNT<br/>PK: ACCOUNT_ID<br/>UK: ACCOUNT_NUMBER<br/>Ref: CUSTOMER_ID"]
        AL["ACCOUNT_LEDGER<br/>Ref: TRANSACTION_REFERENCE"]
    end

    subgraph TRANSACTION_SCHEMA["Transaction Service"]
        TX["TRANSACTIONS<br/>PK: TRANSACTION_ID<br/>UK: TRANSACTION_REFERENCE<br/>Ref: SOURCE_ACCOUNT_ID<br/>Ref: DEST_ACCOUNT_ID<br/>Ref: CUSTOMER_ID"]
    end

    subgraph NOTIFICATION_SCHEMA["Notification Service"]
        NT["NOTIFICATIONS<br/>Ref: CUSTOMER_ID"]
        AL2["AUDIT_LOGS<br/>Ref: CUSTOMER_ID<br/>Ref: ACTOR_ID"]
    end

    %% Logical Relationships
    AU -. "1 : 1 (Logical by CUSTOMER_ID)" .- CU
    CU -. "1 : N (Logical by CUSTOMER_ID)" .- AC
    CU -. "1 : N (Logical by CUSTOMER_ID)" .- TX
    AC -. "1 : N (Logical by ACCOUNT_ID)" .- TX
    TX -. "1 : N (Logical by TRANSACTION_REFERENCE)" .- AL
    CU -. "1 : N (Logical by CUSTOMER_ID)" .- NT
    CU -. "1 : N (Logical by CUSTOMER_ID)" .- AL2
```

### Reference Resolution Matrix

| Referencing Service | Table & Column | Target Service | Referenced Entity | Resolution Mechanism |
| :--- | :--- | :--- | :--- | :--- |
| **Auth** | `AUTH_USER.CUSTOMER_ID` | User Service | `CUSTOMER.CUSTOMER_ID` | Registration event / JWT payload |
| **Account** | `ACCOUNT.CUSTOMER_ID` | User Service | `CUSTOMER.CUSTOMER_ID` | OpenFeign during account approval |
| **Account** | `ACCOUNT_LEDGER.TRANSACTION_REFERENCE` | Transaction Service | `TRANSACTIONS.TRANSACTION_REFERENCE` | OpenFeign / Saga transfer request |
| **Transaction** | `TRANSACTIONS.CUSTOMER_ID` | User Service | `CUSTOMER.CUSTOMER_ID` | JWT Claim (`customerId`) |
| **Transaction** | `TRANSACTIONS.SOURCE_ACCOUNT_ID` | Account Service | `ACCOUNT.ACCOUNT_ID` | OpenFeign verification & balance check |
| **Transaction** | `TRANSACTIONS.DESTINATION_ACCOUNT_ID` | Account Service | `ACCOUNT.ACCOUNT_ID` | OpenFeign beneficiary verification |
| **Notification** | `NOTIFICATIONS.CUSTOMER_ID` | User Service | `CUSTOMER.CUSTOMER_ID` | Kafka domain event payload |
| **Notification** | `AUDIT_LOGS.CUSTOMER_ID` | User Service | `CUSTOMER.CUSTOMER_ID` | Kafka audit event stream |
| **Notification** | `AUDIT_LOGS.ACTOR_ID` | Auth / User Service | `AUTH_USER.USER_ID` or Admin username | JWT subject / security context |
