-- ============================================================
-- NETBANKING PROJECT - TRANSACTION SERVICE
-- Final Oracle schema based on:
--   * Functional Requirements (FuncReq.pdf)
--   * Auth Service boundary
--   * User Service boundary
--   * Account Service boundary
--   * Notification Service/Kafka boundary
--   * TransactSphere reference architecture
--
-- Run this script after create_schema.sql and
-- connect as TRANSACTION_SCHEMA.
-- ============================================================

-- ============================================================
-- SERVICE OWNERSHIP
-- ============================================================
-- AUTH SERVICE:
--   Registration, login/logout, password, MFA/OTP, JWT, roles.
--
-- USER SERVICE:
--   Customer/profile data and admin/customer dashboard APIs.
--
-- ACCOUNT SERVICE:
--   Account creation/state, account PIN, balance, available balance,
--   debit, credit, ledger, freeze/block/unblock/close/type changes.
--
-- TRANSACTION SERVICE:
--   Transfers, self transfers, transaction lifecycle/history,
--   idempotency and statement generation/orchestration.
--
-- NOTIFICATION SERVICE:
--   Kafka consumers and email notifications through Google SMTP.
--
-- Cross-service identifiers are application-level references only.
-- No FK is created to another microservice database/schema.
-- ============================================================


-- ============================================================
-- 1. SEQUENCES
-- ============================================================

CREATE SEQUENCE SEQ_TRANSACTION_ID
    START WITH 100001
    INCREMENT BY 1
    NOCACHE
    NOCYCLE;

CREATE SEQUENCE SEQ_TRANSFER_ID
    START WITH 200001
    INCREMENT BY 1
    NOCACHE
    NOCYCLE;

CREATE SEQUENCE SEQ_STATUS_HISTORY_ID
    START WITH 300001
    INCREMENT BY 1
    NOCACHE
    NOCYCLE;

CREATE SEQUENCE SEQ_IDEMPOTENCY_ID
    START WITH 400001
    INCREMENT BY 1
    NOCACHE
    NOCYCLE;

CREATE SEQUENCE SEQ_STATEMENT_REQUEST_ID
    START WITH 500001
    INCREMENT BY 1
    NOCACHE
    NOCYCLE;


-- ============================================================
-- 2. TRANSACTIONS
-- ============================================================
-- Main transaction record.
-- Account Service remains the source of truth for money, balance
-- and ledger. This table stores transaction metadata/workflow.
-- ============================================================

CREATE TABLE TRANSACTIONS (
    TRANSACTION_ID         NUMBER(19) DEFAULT SEQ_TRANSACTION_ID.NEXTVAL PRIMARY KEY,
    TRANSACTION_REFERENCE  VARCHAR2(100) NOT NULL,

    TRANSACTION_TYPE       VARCHAR2(30) NOT NULL,
    -- FUND_TRANSFER / SELF_TRANSFER / CUSTOMER_TRANSFER

    TRANSACTION_STATUS     VARCHAR2(20) NOT NULL,
    -- INITIATED / PROCESSING / SUCCESS / FAILED / REVERSED

    SOURCE_ACCOUNT_ID      NUMBER(19) NOT NULL,
    DESTINATION_ACCOUNT_ID NUMBER(19) NOT NULL,
    CUSTOMER_ID            VARCHAR2(50) NOT NULL,
    INITIATED_BY           VARCHAR2(50) NOT NULL,

    AMOUNT                 NUMBER(19,2) NOT NULL,
    CURRENCY               VARCHAR2(3) DEFAULT 'INR' NOT NULL,

    DESCRIPTION            VARCHAR2(255),
    FAILURE_REASON         VARCHAR2(500),

    INITIATED_AT           TIMESTAMP DEFAULT SYSTIMESTAMP NOT NULL,
    COMPLETED_AT           TIMESTAMP,
    CREATED_AT             TIMESTAMP DEFAULT SYSTIMESTAMP NOT NULL,
    UPDATED_AT             TIMESTAMP DEFAULT SYSTIMESTAMP NOT NULL,

    CONSTRAINT UQ_TXN_REFERENCE
        UNIQUE (TRANSACTION_REFERENCE),

    CONSTRAINT CHK_TXN_TYPE
        CHECK (TRANSACTION_TYPE IN (
            'FUND_TRANSFER',
            'SELF_TRANSFER',
            'CUSTOMER_TRANSFER'
        )),

    CONSTRAINT CHK_TXN_STATUS
        CHECK (TRANSACTION_STATUS IN (
            'INITIATED',
            'PROCESSING',
            'SUCCESS',
            'FAILED',
            'REVERSED'
        )),

    CONSTRAINT CHK_TXN_AMOUNT
        CHECK (AMOUNT > 0),

    CONSTRAINT CHK_TXN_CURRENCY
        CHECK (LENGTH(CURRENCY) = 3),

    CONSTRAINT CHK_TXN_COMPLETED_AT
        CHECK (
            (TRANSACTION_STATUS IN ('SUCCESS', 'FAILED', 'REVERSED')
             AND COMPLETED_AT IS NOT NULL)
            OR
            (TRANSACTION_STATUS IN ('INITIATED', 'PROCESSING')
             AND COMPLETED_AT IS NULL)
        )
);


-- ============================================================
-- 3. TRANSFER_DETAILS
-- ============================================================
-- INTERNAL = transfer to another customer's account.
-- SELF     = transfer between the same customer's own accounts.
-- ============================================================

CREATE TABLE TRANSFER_DETAILS (
    TRANSFER_ID       NUMBER(19) DEFAULT SEQ_TRANSFER_ID.NEXTVAL PRIMARY KEY,
    TRANSACTION_ID    NUMBER(19) NOT NULL,
    TRANSFER_TYPE     VARCHAR2(30) NOT NULL,
    TRANSFER_MODE     VARCHAR2(20) DEFAULT 'IMMEDIATE' NOT NULL,
    REMARKS           VARCHAR2(255),
    SCHEDULED_AT      TIMESTAMP,
    CREATED_AT        TIMESTAMP DEFAULT SYSTIMESTAMP NOT NULL,

    CONSTRAINT UQ_TRANSFER_TRANSACTION
        UNIQUE (TRANSACTION_ID),

    CONSTRAINT FK_TRANSFER_TRANSACTION
        FOREIGN KEY (TRANSACTION_ID)
        REFERENCES TRANSACTIONS (TRANSACTION_ID),

    CONSTRAINT CHK_TRANSFER_TYPE
        CHECK (TRANSFER_TYPE IN (
            'INTERNAL',
            'SELF',
            'FUND_TRANSFER',
            'SELF_TRANSFER',
            'CUSTOMER_TRANSFER'
        )),

    CONSTRAINT CHK_TRANSFER_MODE
        CHECK (TRANSFER_MODE IN (
            'IMMEDIATE',
            'SCHEDULED'
        )),

    CONSTRAINT CHK_TRANSFER_SCHEDULED_AT
        CHECK (
            (TRANSFER_MODE = 'SCHEDULED' AND SCHEDULED_AT IS NOT NULL)
            OR
            (TRANSFER_MODE = 'IMMEDIATE' AND SCHEDULED_AT IS NULL)
        )
);


-- ============================================================
-- 4. TRANSACTION_STATUS_HISTORY
-- ============================================================
-- Immutable-style lifecycle history for auditability.
-- ============================================================

CREATE TABLE TRANSACTION_STATUS_HISTORY (
    HISTORY_ID       NUMBER(19) DEFAULT SEQ_STATUS_HISTORY_ID.NEXTVAL PRIMARY KEY,
    TRANSACTION_ID   NUMBER(19) NOT NULL,
    PREVIOUS_STATUS  VARCHAR2(20),
    NEW_STATUS       VARCHAR2(20) NOT NULL,
    REASON           VARCHAR2(500),
    CHANGED_AT       TIMESTAMP DEFAULT SYSTIMESTAMP NOT NULL,

    CONSTRAINT FK_STATUS_HISTORY_TXN
        FOREIGN KEY (TRANSACTION_ID)
        REFERENCES TRANSACTIONS (TRANSACTION_ID),

    CONSTRAINT CHK_STATUS_HISTORY_NEW
        CHECK (NEW_STATUS IN (
            'INITIATED',
            'PROCESSING',
            'SUCCESS',
            'FAILED',
            'REVERSED'
        )),

    CONSTRAINT CHK_STATUS_HISTORY_PREV
        CHECK (
            PREVIOUS_STATUS IS NULL
            OR PREVIOUS_STATUS IN (
                'INITIATED',
                'PROCESSING',
                'SUCCESS',
                'FAILED',
                'REVERSED'
            )
        )
);


-- ============================================================
-- 5. IDEMPOTENCY_RECORDS
-- ============================================================
-- Recommended reliability control. Prevents duplicate transfers
-- when a client retries an already-submitted transfer request.
-- ============================================================

CREATE TABLE IDEMPOTENCY_RECORDS (
    IDEMPOTENCY_ID    NUMBER(19) DEFAULT SEQ_IDEMPOTENCY_ID.NEXTVAL PRIMARY KEY,
    IDEMPOTENCY_KEY   VARCHAR2(100) NOT NULL,
    CUSTOMER_ID       VARCHAR2(50) NOT NULL,
    TRANSACTION_ID    NUMBER(19),
    REQUEST_HASH      VARCHAR2(128),
    STATUS            VARCHAR2(20) NOT NULL,
    CREATED_AT        TIMESTAMP DEFAULT SYSTIMESTAMP NOT NULL,
    EXPIRES_AT        TIMESTAMP,

    CONSTRAINT UQ_IDEMPOTENCY_KEY
        UNIQUE (IDEMPOTENCY_KEY),

    CONSTRAINT FK_IDEMPOTENCY_TXN
        FOREIGN KEY (TRANSACTION_ID)
        REFERENCES TRANSACTIONS (TRANSACTION_ID),

    CONSTRAINT CHK_IDEMPOTENCY_STATUS
        CHECK (STATUS IN (
            'PROCESSING',
            'COMPLETED',
            'FAILED'
        ))
);


-- ============================================================
-- 6. STATEMENT_REQUESTS
-- ============================================================
-- Functional requirement: customer can download account statement.
-- Transaction Service owns the statement API/generation workflow.
-- Ledger data remains in Account Service and is fetched through
-- service-to-service communication; it is NOT duplicated here.
-- ============================================================

CREATE TABLE STATEMENT_REQUESTS (
    REQUEST_ID       NUMBER(19) DEFAULT SEQ_STATEMENT_REQUEST_ID.NEXTVAL PRIMARY KEY,
    ACCOUNT_ID       NUMBER(19) NOT NULL,
    CUSTOMER_ID      VARCHAR2(50) NOT NULL,
    REQUESTED_BY     VARCHAR2(50) NOT NULL,

    REQUEST_TYPE     VARCHAR2(10) NOT NULL,
    -- PDF / CSV

    FROM_DATE        TIMESTAMP,
    TO_DATE          TIMESTAMP,

    STATUS           VARCHAR2(20) NOT NULL,
    -- PROCESSING / COMPLETED / FAILED

    FILE_NAME        VARCHAR2(255),
    FILE_URL         VARCHAR2(500),

    REQUESTED_AT     TIMESTAMP DEFAULT SYSTIMESTAMP NOT NULL,
    COMPLETED_AT     TIMESTAMP,
    FAILURE_REASON   VARCHAR2(500),

    CONSTRAINT CHK_STATEMENT_TYPE
        CHECK (REQUEST_TYPE IN (
            'PDF',
            'CSV'
        )),

    CONSTRAINT CHK_STATEMENT_STATUS
        CHECK (STATUS IN (
            'PROCESSING',
            'COMPLETED',
            'FAILED'
        )),

    CONSTRAINT CHK_STATEMENT_DATE_RANGE
        CHECK (
            FROM_DATE IS NULL
            OR TO_DATE IS NULL
            OR FROM_DATE <= TO_DATE
        ),

    CONSTRAINT CHK_STATEMENT_COMPLETED_AT
        CHECK (
            (STATUS IN ('COMPLETED', 'FAILED')
             AND COMPLETED_AT IS NOT NULL)
            OR
            (STATUS = 'PROCESSING'
             AND COMPLETED_AT IS NULL)
        )
);


-- ============================================================
-- 7. INDEXES
-- ============================================================

CREATE INDEX IDX_TXN_SOURCE_ACCOUNT
    ON TRANSACTIONS (SOURCE_ACCOUNT_ID);

CREATE INDEX IDX_TXN_DEST_ACCOUNT
    ON TRANSACTIONS (DESTINATION_ACCOUNT_ID);

CREATE INDEX IDX_TXN_CUSTOMER
    ON TRANSACTIONS (CUSTOMER_ID);

CREATE INDEX IDX_TXN_INITIATED_BY
    ON TRANSACTIONS (INITIATED_BY);

CREATE INDEX IDX_TXN_STATUS
    ON TRANSACTIONS (TRANSACTION_STATUS);

CREATE INDEX IDX_TXN_INITIATED_AT
    ON TRANSACTIONS (INITIATED_AT);

CREATE INDEX IDX_TRANSFER_TXN
    ON TRANSFER_DETAILS (TRANSACTION_ID);

CREATE INDEX IDX_STATUS_HISTORY_TXN
    ON TRANSACTION_STATUS_HISTORY (TRANSACTION_ID);

CREATE INDEX IDX_STATUS_HISTORY_CHANGED_AT
    ON TRANSACTION_STATUS_HISTORY (CHANGED_AT);

CREATE INDEX IDX_IDEMPOTENCY_CUSTOMER
    ON IDEMPOTENCY_RECORDS (CUSTOMER_ID);

CREATE INDEX IDX_IDEMPOTENCY_TXN
    ON IDEMPOTENCY_RECORDS (TRANSACTION_ID);

CREATE INDEX IDX_IDEMPOTENCY_EXPIRES
    ON IDEMPOTENCY_RECORDS (EXPIRES_AT);

CREATE INDEX IDX_STATEMENT_ACCOUNT
    ON STATEMENT_REQUESTS (ACCOUNT_ID);

CREATE INDEX IDX_STATEMENT_CUSTOMER
    ON STATEMENT_REQUESTS (CUSTOMER_ID);

CREATE INDEX IDX_STATEMENT_REQUESTED_AT
    ON STATEMENT_REQUESTS (REQUESTED_AT);


-- ============================================================
-- 8. TRANSACTION STATUS TRIGGER
-- ============================================================
-- Keeps UPDATED_AT current when a transaction row changes.
-- ============================================================

CREATE OR REPLACE TRIGGER TRG_TXN_UPDATED_AT
BEFORE UPDATE ON TRANSACTIONS
FOR EACH ROW
BEGIN
    :NEW.UPDATED_AT := SYSTIMESTAMP;
END;
/


-- ============================================================
-- 9. BENEFICIARY / ACCOUNT / CUSTOMER REFERENCES
-- ============================================================
-- No database foreign keys are intentionally created for:
--   TRANSACTIONS.CUSTOMER_ID
--   TRANSACTIONS.INITIATED_BY
--   TRANSACTIONS.SOURCE_ACCOUNT_ID
--   TRANSACTIONS.DESTINATION_ACCOUNT_ID
--   IDEMPOTENCY_RECORDS.CUSTOMER_ID
--   STATEMENT_REQUESTS.ACCOUNT_ID
--   STATEMENT_REQUESTS.CUSTOMER_ID
--   STATEMENT_REQUESTS.REQUESTED_BY
--
-- These belong to other services and are resolved using APIs/events.
--
-- This avoids violating database-per-service ownership and avoids
-- hard coupling Transaction Service to Auth/User/Account schemas.
-- ============================================================


-- ============================================================
-- 10. VERIFICATION QUERIES
-- ============================================================

SELECT TABLE_NAME
FROM USER_TABLES
WHERE TABLE_NAME IN (
    'TRANSACTIONS',
    'TRANSFER_DETAILS',
    'TRANSACTION_STATUS_HISTORY',
    'IDEMPOTENCY_RECORDS',
    'STATEMENT_REQUESTS'
)
ORDER BY TABLE_NAME;

SELECT SEQUENCE_NAME
FROM USER_SEQUENCES
WHERE SEQUENCE_NAME IN (
    'SEQ_TRANSACTION_ID',
    'SEQ_TRANSFER_ID',
    'SEQ_STATUS_HISTORY_ID',
    'SEQ_IDEMPOTENCY_ID',
    'SEQ_STATEMENT_REQUEST_ID'
)
ORDER BY SEQUENCE_NAME;

SELECT TABLE_NAME, CONSTRAINT_NAME, CONSTRAINT_TYPE, STATUS
FROM USER_CONSTRAINTS
WHERE TABLE_NAME IN (
    'TRANSACTIONS',
    'TRANSFER_DETAILS',
    'TRANSACTION_STATUS_HISTORY',
    'IDEMPOTENCY_RECORDS',
    'STATEMENT_REQUESTS'
)
ORDER BY TABLE_NAME, CONSTRAINT_NAME;

SELECT INDEX_NAME, TABLE_NAME, STATUS
FROM USER_INDEXES
WHERE TABLE_NAME IN (
    'TRANSACTIONS',
    'TRANSFER_DETAILS',
    'TRANSACTION_STATUS_HISTORY',
    'IDEMPOTENCY_RECORDS',
    'STATEMENT_REQUESTS'
)
ORDER BY TABLE_NAME, INDEX_NAME;

COMMIT;
