<div align="center">

# NetBanking Microservices Platform

### A modern, event-driven digital banking demo

[![Java](https://img.shields.io/badge/Java-17-8B1E1E?style=for-the-badge&logo=openjdk&logoColor=white)](https://www.oracle.com/java/)
[![Spring Boot](https://img.shields.io/badge/Spring_Boot-3.5-1F6B4A?style=for-the-badge&logo=springboot&logoColor=white)](https://spring.io/projects/spring-boot)
[![Oracle JET](https://img.shields.io/badge/Oracle_JET-Frontend-C74634?style=for-the-badge&logo=oracle&logoColor=white)](https://www.oracle.com/webfolder/technetwork/jet/index.html)
[![Maven](https://img.shields.io/badge/Maven-Build-5E2D79?style=for-the-badge&logo=apachemaven&logoColor=white)](https://maven.apache.org/)

`Java 17` • `Spring Boot 3.5` • `Oracle JET` • `Kafka` • `Podman`

</div>

> [!NOTE]
> A beginner-friendly banking platform built with Spring Boot microservices and an Oracle JET frontend. It demonstrates registration, JWT authentication, account operations, transactions, notifications, monitoring, and service discovery.

## ✦ Overview

The application is split by business domain, so each service can be developed and run independently while the API gateway offers one entry point for clients.

| Capability | Included |
| --- | --- |
| **Identity** | User registration, login, JWT, and OTP flows |
| **Core banking** | Customer profiles, accounts, balances, transfers, and statements |
| **Platform** | Eureka discovery, gateway routing, Redis rate limiting, Kafka events |
| **Observability** | Actuator, Prometheus, and Grafana dashboards |
| **Experience** | Oracle JET banking portal |

## ✦ Architecture at a glance

| Component | Port | Responsibility |
| --- | ---: | --- |
| **Frontend** | `8000` | Oracle JET web application |
| **API Gateway** | `8080` | Client entry point, routing, and rate limiting |
| **Eureka Server** | `8761` | Service discovery |
| **Auth Service** | `8081` | Registration, login, JWT, and OTP |
| **User Service** | `8082` | Customer profile data |
| **Account Service** | `8083` | Accounts and balances |
| **Transaction Service** | `8084` | Transfers and statements |
| **Notification Service** | `8085` | Email and notification flows |
| **Prometheus** | `9090` | Metrics collection |
| **Grafana** | `3000` | Metrics dashboards |

## ✦ Technology

| Layer | Technology |
| --- | --- |
| Language | Java 17 |
| Backend | Spring Boot 3.5, Spring Cloud Gateway, Spring Security |
| Data | Oracle JDBC / JPA |
| Events and cache | Kafka, Redis |
| Discovery | Eureka |
| Frontend | Oracle JET |
| Tooling | Maven, Podman |
| Observability | Micrometer, Prometheus, Grafana |
| API documentation | SpringDoc OpenAPI / Swagger UI |

## ✦ Prerequisites

- Java 17+ and Maven 3.9+
- Node.js 16+ and npm
- Oracle Database with a running `FREEPDB1` instance
- Kafka at `localhost:9092` and Redis at `localhost:6379`
- Podman *(for the included container startup scripts)*
- Git

## ✦ Get started

### 1. Clone the repository

```bash
git clone <your-repo-url>
cd NetBanking
```

### 2. Configure your environment

```bash
# Linux / macOS
cp .env.example .env
```

```powershell
# Windows PowerShell
Copy-Item .env.example .env
```

Update `.env` with your local credentials and service configuration:

```env
JWT_SECRET=your_base64_encoded_256_bit_secret_key_here
AUTH_DB_URL=jdbc:oracle:thin:@localhost:1521/FREEPDB1
AUTH_DB_USERNAME=AUTH_SCHEMA
AUTH_DB_PASSWORD=your_password
KAFKA_BOOTSTRAP_SERVERS=localhost:9092
REDIS_HOST=localhost
REDIS_PORT=6379
```

> [!IMPORTANT]
> Never commit `.env` files or real secrets. Use a strong, shared `JWT_SECRET` in both the Auth Service and API Gateway.

### 3. Install and build

```bash
cd netbanking-frontend
npm install
```

```bash
cd Model_Class/eureka-server && mvn clean install
cd ../auth-service && mvn clean install
cd ../User-Service && mvn clean install
cd ../Account-Service && mvn clean install
cd ../Transaction-service && mvn clean install
cd ../Notification-Service && mvn clean install
cd ../api-gateway && mvn clean install
```

## ✦ Run the platform

### One-click startup — Windows

```powershell
./start_netbanking.ps1
```

Or run `start_netbanking.bat`. This starts Podman infrastructure, Eureka, backend services, gateway, and frontend.

### Manual startup

Start Eureka first:

```bash
cd Model_Class/eureka-server
mvn spring-boot:run
```

Then, in separate terminals, start each service:

```bash
cd Model_Class/auth-service && mvn spring-boot:run
cd Model_Class/User-Service && mvn spring-boot:run
cd Model_Class/Account-Service && mvn spring-boot:run
cd Model_Class/Transaction-service && mvn spring-boot:run
cd Model_Class/Notification-Service && mvn spring-boot:run
cd Model_Class/api-gateway && mvn spring-boot:run
```

Finally, start the frontend:

```bash
cd netbanking-frontend
node server.js
```

### Stop the platform

```powershell
./stop_netbanking.ps1
# Add -Containers to stop infrastructure too
./stop_netbanking.ps1 -Containers
```

## ✦ Configuration

Configuration templates:

- `.env.example`
- `Model_Class/api-gateway/.env.example`
- `Model_Class/auth-service/.env.example`

| Variable | Purpose |
| --- | --- |
| `JWT_SECRET` | Signs and validates authentication tokens |
| `AUTH_DB_URL`, `USER_DB_URL`, `ACCOUNT_DB_URL` | Service database connections |
| `EUREKA_URI` | Eureka server address |
| `KAFKA_BOOTSTRAP_SERVERS` | Kafka broker address |
| `REDIS_HOST`, `REDIS_PORT` | Redis connection for gateway rate limiting |

```env
EUREKA_URI=http://localhost:8761/eureka/
KAFKA_BOOTSTRAP_SERVERS=localhost:9092
REDIS_HOST=localhost
REDIS_PORT=6379
```

## ✦ Local endpoints

| Service | URL |
| --- | --- |
| Frontend | http://localhost:8000 |
| API Gateway | http://localhost:8080 |
| Eureka Dashboard | http://localhost:8761 |
| Auth Swagger | http://localhost:8081/swagger-ui/index.html |
| User Swagger | http://localhost:8082/swagger-ui/index.html |
| Account Swagger | http://localhost:8083/swagger-ui/index.html |
| Transaction Swagger | http://localhost:8084/swagger-ui/index.html |
| Notification Swagger | http://localhost:8085/swagger-ui/index.html |
| Prometheus | http://localhost:9090 |
| Grafana | http://localhost:3000 |

## ✦ API examples

### Register a user

```bash
curl -X POST "http://localhost:8080/api/v1/auth/register" \
  -H "Content-Type: application/json" \
  -d '{"email":"customer@example.com","password":"StrongPassword123!"}'
```

### Log in

```bash
curl -X POST "http://localhost:8080/api/v1/auth/login" \
  -H "Content-Type: application/json" \
  -d '{"email":"customer@example.com","password":"StrongPassword123!"}'
```

### Check service health

```bash
curl http://localhost:8080/actuator/health
```

## ✦ Monitoring

Every service exposes Micrometer metrics at `/actuator/prometheus`. The repository includes:

- Prometheus configuration: `Monitoring/prometheus.yml`
- Grafana dashboard: `Monitoring/netbanking-frontend-operations-dashboard.json`

```bash
cd Monitoring
prometheus --config.file=prometheus.yml
```

```bash
podman run -d --name prometheus -p 9090:9090 \
  -v "${PWD}/Monitoring/prometheus.yml:/etc/prometheus/prometheus.yml" \
  quay.io/prometheus/prometheus

podman run -d --name grafana -p 3000:3000 quay.io/containers/grafana
```

Add Prometheus as a Grafana data source, then import the included dashboard to view login, account, transaction, and notification metrics.

## ✦ Tests

Run tests from any service directory:

```bash
cd Model_Class/auth-service
mvn test
```

## ✦ Project structure

```text
NetBanking/
├── .env.example
├── Documentation/                 # API contracts and guides
├── Model_Class/
│   ├── api-gateway/
│   ├── auth-service/
│   ├── eureka-server/
│   ├── User-Service/
│   ├── Account-Service/
│   ├── Transaction-service/
│   └── Notification-Service/
├── netbanking-frontend/           # Oracle JET application
├── Monitoring/                    # Prometheus and Grafana assets
├── Schemas/
├── start_netbanking.ps1
├── stop_netbanking.ps1
└── ER_Diagram.md
```

## ✦ Troubleshooting

| Symptom | Check |
| --- | --- |
| Services do not start | Java, Maven, Oracle DB, Kafka, Redis, and `.env` values |
| Oracle connection error | Host, port, `FREEPDB1`, credentials, and Oracle listener |
| Kafka connection refused | Kafka is running at `localhost:9092` |
| Redis rate-limit error | Redis is running at `localhost:6379`; gateway settings are correct |
| Frontend does not load | Run `npm install`, then `node server.js` inside `netbanking-frontend` |
| Port is occupied | Check ports `8000`, `8080`–`8085`, and `8761` |
| JWT validation fails | Use the exact same `JWT_SECRET` in Auth Service and API Gateway |

Expected Oracle JDBC format:

```text
jdbc:oracle:thin:@localhost:1521/FREEPDB1
```

## ✦ Deployment

This repository currently targets local development and demo environments. The included scripts run Podman-managed Oracle DB, Kafka, and Redis alongside the local services.

> [!TIP]
> For production, add CI/CD configuration, container images, secret management, and deployment manifests for your chosen platform.

## ✦ Contributing

Contributions are welcome. Add a `CONTRIBUTING.md` document with coding standards, review expectations, and branch workflow before accepting external contributions.

## ✦ License

No license file is currently included. Add a `LICENSE` file before public distribution or production use.

---

<div align="center">

Built as a learning and demo platform for exploring modern banking microservices.

</div>
