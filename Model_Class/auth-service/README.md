# NetBanking Auth Service

Authentication service aligned to the existing NetBanking AUTH_SCHEMA and service boundaries.

## Responsibilities

- Registration and customer identity reference generation
- BCrypt password hashing
- Customer role assignment
- Login with optional email OTP/MFA challenge
- 15-minute RSA-signed JWT access tokens
- 7-day rotating refresh tokens, stored as SHA-256 hashes
- Logout/revocation
- Forgot/reset password tokens
- Email OTP storage and verification
- Login history
- Eureka client registration as `auth-service` on port 8081
- Kafka domain events for downstream Notification/Audit processing
- Public JWK endpoint for API Gateway JWT validation

The database schema is treated as pre-created. Hibernate uses `ddl-auto=validate` and will not create or alter the tables.

## Required local components

- Oracle Free / FreePDB1 with `AUTH_SCHEMA`
- Eureka Server on `8761`
- Kafka on `9092`

Redis is not required by Auth Service itself; Redis is used by the API Gateway for rate limiting.

## Environment Configuration & Secrets

All secrets and credentials are loaded dynamically from `.env` via `DotenvLoader`:

```text
AUTH_DB_URL=jdbc:oracle:thin:@localhost:1521/FREEPDB1
AUTH_DB_USERNAME=AUTH_SCHEMA
AUTH_DB_PASSWORD=your_auth_password
JWT_SECRET=your-256-bit-secret-base64-encoded
```

See `.env.example` for the full list of configurable environment variables.

## JWT Integration with API Gateway

- Pure HMAC-SHA256 tokens using `io.jsonwebtoken` (JJWT 0.12.7).
- Shared `JWT_SECRET` loaded from `.env` into both `auth-service` and `api-gateway`.
- API Gateway validates incoming Bearer JWTs at the edge and propagates trusted user context (`X-Customer-Id`, `X-User-Email`, `X-User-Roles`) to downstream services.

## Endpoints

```text
POST /api/auth/register
POST /api/auth/login
POST /api/auth/refresh
POST /api/auth/logout
POST /api/auth/verify-otp
POST /api/auth/forgot-password
POST /api/auth/reset-password
```

Health:

```text
GET /actuator/health
```

## Login flow

1. Register a customer.
2. Verify the email OTP.
3. Submit email + password + LOGIN OTP.
4. Receive access token and refresh token.
5. Send `Authorization: Bearer <access-token>` to the API Gateway for protected APIs.

For a production implementation, OTP delivery should be handled by the Notification Service; Auth emits Kafka events and stores only hashed OTPs in `EMAIL_OTP`.
