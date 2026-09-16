# NetBanking Transaction Service

Spring Boot microservice aligned with the finalized TRANSACTION_SCHEMA.

## Database entities
- TRANSACTIONS
- TRANSFER_DETAILS
- TRANSACTION_STATUS_HISTORY
- IDEMPOTENCY_RECORDS
- STATEMENT_REQUESTS

## Responsibilities
- Fund/self/customer transfer orchestration
- Transaction lifecycle and status history
- Idempotency records
- Statement request workflow
- Account Service integration via OpenFeign
- User Service integration via OpenFeign
- Transaction event publication through Kafka

## Important boundary
Account Service remains the source of truth for account balances and ledger operations. Transaction Service keeps account/customer identifiers as logical references and does not create cross-service database foreign keys.

## Run
1. Update `src/main/resources/application.yml` with your Oracle credentials and service URLs.
2. Make sure the `TRANSACTION_SCHEMA` objects from the approved SQL script already exist.
3. Run `mvn spring-boot:run`.

## Current implementation note
The transfer flow contains the initial orchestration path, but production-grade compensation/reversal for a successful debit followed by a failed credit should be implemented as the next reliability milestone. Statement file generation is also scaffolded and should be completed against Account Service ledger APIs.
