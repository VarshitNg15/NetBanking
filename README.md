# NetBanking Microservices Platform

A beginner-friendly banking demo built with Spring Boot microservices and an Oracle JET frontend. The application includes authentication, user management, account operations, transactions, and notifications.

## What this project does

This project simulates a digital banking platform with:

- User registration and login
- JWT-based authentication
- API gateway routing and rate limiting
- Customer and account management
- Transaction processing and ledger flows
- Notification and audit events
- Service discovery with Eureka
- Oracle JET frontend UI

The backend is split into separate services so each business area can be developed and run independently.

## Architecture at a glance

| Component | Port | Purpose |
| --- | ---: | --- |
| Frontend | 8000 | Oracle JET web app |
| API Gateway | 8080 | Main entry point for client requests |
| Eureka Server | 8761 | Service discovery dashboard |
| Auth Service | 8081 | Register, login, JWT, OTP |
| User Service | 8082 | Customer profile data |
| Account Service | 8083 | Accounts and balances |
| Transaction Service | 8084 | Transfers and statements |
| Notification Service | 8085 | Email/notification flows |

## Prerequisites

Before you start, make sure you have:

- Java 17+ and Maven 3.9+
- Node.js 16+ and npm
- Oracle database with a running FreePDB1 instance
- Kafka broker on `localhost:9092`
- Redis on `localhost:6379`
- Podman (if you want to use the included container startup scripts)
- Git

## Installation

1. Clone the repository:

```bash
git clone <your-repo-url>
cd NetBanking
```

2. Create your environment file from the template:

On Linux/macOS:

```bash
cp .env.example .env
```

On Windows PowerShell:

```powershell
Copy-Item .env.example .env
```

3. Edit `.env` and replace placeholders with your database, JWT, and service settings.

Example values:

```env
JWT_SECRET=your_base64_encoded_256_bit_secret_key_here
AUTH_DB_URL=jdbc:oracle:thin:@localhost:1521/FREEPDB1
AUTH_DB_USERNAME=AUTH_SCHEMA
AUTH_DB_PASSWORD=your_password
KAFKA_BOOTSTRAP_SERVERS=localhost:9092
REDIS_HOST=localhost
REDIS_PORT=6379
```

4. Install frontend dependencies:

```bash
cd netbanking-frontend
npm install
```

5. Build Java services (optional but recommended):

```bash
cd Model_Class/eureka-server && mvn clean install
cd ../auth-service && mvn clean install
cd ../User-Service && mvn clean install
cd ../Account-Service && mvn clean install
cd ../Transaction-service && mvn clean install
cd ../Notification-Service && mvn clean install
cd ../api-gateway && mvn clean install
```

## Running the project

### Option 1: One-click startup (recommended)

On Windows PowerShell:

```powershell
./start_netbanking.ps1
```

Or double-click:

```powershell
start_netbanking.bat
```

This script starts:

- Podman infrastructure containers
- Eureka discovery server
- Backend microservices
- API gateway
- Frontend app

### Option 2: Run services manually

Start discovery first:

```bash
cd Model_Class/eureka-server
mvn spring-boot:run
```

Then start each service in its own terminal:

```bash
cd Model_Class/auth-service
mvn spring-boot:run
```

```bash
cd "Model_Class/User-Service"
mvn spring-boot:run
```

```bash
cd "Model_Class/Account-Service"
mvn spring-boot:run
```

```bash
cd "Model_Class/Transaction-service"
mvn spring-boot:run
```

```bash
cd "Model_Class/Notification-Service"
mvn spring-boot:run
```

```bash
cd "Model_Class/api-gateway"
mvn spring-boot:run
```

Finally, start the frontend:

```bash
cd netbanking-frontend
node server.js
```

## Default URLs

After startup, the project is typically available at:

- Frontend: http://localhost:8000
- API Gateway: http://localhost:8080
- Eureka Dashboard: http://localhost:8761
- Auth Swagger: http://localhost:8081/swagger-ui/index.html
- User Swagger: http://localhost:8082/swagger-ui/index.html
- Account Swagger: http://localhost:8083/swagger-ui/index.html
- Transaction Swagger: http://localhost:8084/swagger-ui/index.html
- Notification Swagger: http://localhost:8085/swagger-ui/index.html

## Configuration

The project uses environment variables for runtime settings. The main templates are:

- `.env.example` at the project root
- `Model_Class/api-gateway/.env.example`
- `Model_Class/auth-service/.env.example`

Important settings include:

- `JWT_SECRET` for auth and gateway validation
- `AUTH_DB_URL`, `USER_DB_URL`, `ACCOUNT_DB_URL`, etc.
- `KAFKA_BOOTSTRAP_SERVERS`
- `REDIS_HOST` and `REDIS_PORT`
- `EUREKA_URI`

A common example is:

```env
JWT_SECRET=your_base64_encoded_256_bit_secret_key_here
EUREKA_URI=http://localhost:8761/eureka/
KAFKA_BOOTSTRAP_SERVERS=localhost:9092
REDIS_HOST=localhost
REDIS_PORT=6379
```

## Usage examples

### Register a user

```bash
curl -X POST "http://localhost:8080/api/v1/auth/register" \
  -H "Content-Type: application/json" \
  -d '{"email":"customer@example.com","password":"StrongPassword123!"}'
```

### Login

```bash
curl -X POST "http://localhost:8080/api/v1/auth/login" \
  -H "Content-Type: application/json" \
  -d '{"email":"customer@example.com","password":"StrongPassword123!"}'
```

### Get service health

```bash
curl http://localhost:8080/actuator/health
```

### Access the frontend

Open in a browser:

```text
http://localhost:8000
```

## Project structure

```text
NetBanking/
├── .env.example
├── Documentation/
│   ├── ALL_PROJECT_APIS.md
│   ├── FEIGN_AND_COMMUNICATION_GUIDE.md
│   ├── openapi.yaml
│   └── openapi.json
├── Model_Class/
│   ├── api-gateway/
│   ├── auth-service/
│   ├── eureka-server/
│   ├── User-Service/
│   ├── Account-Service/
│   ├── Transaction-service/
│   └── Notification-Service/
├── netbanking-frontend/
├── Schemas/
├── start_netbanking.ps1
├── start_netbanking.bat
├── stop_netbanking.ps1
├── stop_netbanking.bat
└── ER_Diagram.md
```

## Common troubleshooting

### 1. Services fail to start

Check that:

- Java and Maven are installed
- the Oracle DB is running
- Kafka and Redis are running
- the `.env` file has valid values

### 2. Database connection errors

Typical causes:

- wrong Oracle hostname or port
- missing `FREEPDB1` service name
- invalid username/password
- Oracle listener not running

Use a connection such as:

```text
jdbc:oracle:thin:@localhost:1521/FREEPDB1
```

### 3. Kafka connection refused

Make sure Kafka is running on port `9092` and that the host matches your environment.

### 4. Redis rate-limiter errors

Check Redis is available on `localhost:6379` and your gateway `.env` is set correctly.

### 5. Frontend does not load

Verify that the frontend dependencies were installed:

```bash
cd netbanking-frontend
npm install
```

Then start:

```bash
node server.js
```

### 6. Port already in use

Common conflicts:

- 8080 gateway
- 8081-8085 service ports
- 8761 Eureka
- 8000 frontend

Stop conflicting apps or change the port in the relevant environment variables.

### 7. JWT errors

Make sure the same `JWT_SECRET` is used in the gateway and auth service. If they differ, API validation fails.

## Stopping the app

```powershell
./stop_netbanking.ps1
```

Or:

```powershell
stop_netbanking.bat
```

## Notes

This project is intended as a learning and demo platform. It uses a microservice architecture with production-like concerns such as JWT validation, service registry, rate limiting, and event-driven communication.

If you are new to the stack, start by running the scripts, then open the Swagger docs and the gateway endpoints to explore the API flow.
