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

## Environment Configuration & JWT Secrets

All secrets and configuration are dynamically loaded from `.env` on startup via `DotenvLoader`:

```text
SERVER_PORT=8080
REDIS_HOST=localhost
REDIS_PORT=6379
JWT_SECRET=your-256-bit-secret-base64-encoded
EUREKA_URI=http://localhost:8761/eureka/
```

- Edge validation uses pure JJWT (HMAC-SHA256) matching `auth-service`'s `JWT_SECRET`.
- On successful validation, trusted headers `X-Customer-Id`, `X-User-Email`, and `X-User-Roles` are forwarded to downstream microservices.
- No OAuth2 Resource Server or remote JWKS roundtrip required.

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
