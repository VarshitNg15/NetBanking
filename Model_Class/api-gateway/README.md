# NetBanking API Gateway

Gateway responsibilities:

- Route requests to Auth, User, Account, Transaction, and Notification services through Eureka.
- Validate Bearer JWTs before protected routes are forwarded.
- Apply Redis-backed token-bucket rate limiting.
- Use the authenticated JWT subject as the rate-limit key; fall back to client IP for public routes.

## Required infrastructure

- Eureka Server: `http://localhost:8761`
- Redis: `localhost:6379`
- Five services registered with Eureka using these service IDs:
  - `auth-service`
  - `user-service`
  - `account-service`
  - `transaction-service`
  - `notification-service`

## JWT configuration

The gateway currently expects:

`JWT_ISSUER_URI=http://localhost:8081`

Replace this with the actual issuer used by Auth Service. The Auth source provided for this project defines credential/session tables, but it does not define a JWT issuer or JWK endpoint, so the value cannot be inferred safely.

If Auth Service exposes a JWK Set endpoint but not provider metadata, uncomment `jwk-set-uri` in `application.yml` and set `JWT_JWK_SET_URI` accordingly.

## Rate limits

- Auth: 5 requests/sec, burst 10
- User: 10 requests/sec, burst 20
- Account: 5 requests/sec, burst 10
- Transaction: 3 requests/sec, burst 6
- Notification: 5 requests/sec, burst 10

These are starter values. Tune them for your application's actual traffic and security requirements.

## Public routes

- `POST /api/auth/login`
- `POST /api/auth/register`
- `POST /api/auth/refresh`
- `POST /api/auth/verify-otp`
- `/actuator/health/**`

All other gateway routes require a valid JWT.

## Example

Client -> `http://localhost:8080/api/accounts/...`

Gateway -> validates JWT -> resolves `account-service` using Eureka -> applies Redis rate limit -> forwards to Account Service.
