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

## Database defaults

```text
URL      jdbc:oracle:thin:@localhost:1521/FREEPDB1
Username AUTH_SCHEMA
Password AuthSchema@123
```

Prefer environment variables in real deployments:

```text
AUTH_DB_URL
AUTH_DB_USERNAME
AUTH_DB_PASSWORD
```

## JWT integration with API Gateway

The service exposes:

```text
GET /.well-known/openid-configuration
GET /.well-known/jwks.json
GET /oauth2/jwks
```

The issuer is `http://localhost:8081` by default. This matches the Gateway's `JWT_ISSUER_URI` default from the companion API Gateway project.

For production, replace the development PEM keys and manage the signing key outside source control. The included PEM keys are for local development only.

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
