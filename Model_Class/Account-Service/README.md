# Account Service

Spring Boot microservice for account lifecycle, balances, PIN verification, and an append-only financial ledger.

## Run

Set an Oracle connection, then start the application:

```bash
export DB_URL='jdbc:oracle:thin:@//localhost:1521/FREEPDB1'
export DB_USERNAME='account_service'
export DB_PASSWORD='account_service'
mvn spring-boot:run
```

Flyway applies the supplied Oracle schema on startup. The API is served at `http://localhost:8080/api/v1/accounts`.

## Main endpoints

| Method | Path | Purpose |
|---|---|---|
| POST | `/api/v1/accounts` | Create an account in `PENDING_APPROVAL` |
| GET | `/api/v1/accounts/{id}` | Get account details |
| GET | `/api/v1/accounts?customerId=...` | List a customer's accounts |
| PATCH | `/api/v1/accounts/{id}/status` | Approve, freeze, block, reject, or close |
| PATCH | `/api/v1/accounts/{id}/type` | Change type and retain history |
| GET | `/api/v1/accounts/{id}/balance` | Get balances |
| POST | `/api/v1/accounts/{id}/credits` | Post a credit |
| POST | `/api/v1/accounts/{id}/debits` | Post a debit |
| GET | `/api/v1/accounts/{id}/ledger` | List ledger entries |
| PUT | `/api/v1/accounts/{id}/pin` | Set a hashed 4-digit PIN |
| POST | `/api/v1/accounts/{id}/pin/verify` | Verify PIN with lockout protection |

## Example create request

```json
{
  "customerId": "CUST-10001",
  "accountType": "SAVINGS",
  "currencyCode": "INR"
}
```

Accounts must be changed to `ACTIVE` before credit or debit postings are accepted. Posting requires a unique `entryReference`, making retries idempotency-safe.
