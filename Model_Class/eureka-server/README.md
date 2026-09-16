# NetBanking Eureka Server

Netflix Eureka service registry for the NetBanking microservices.

## Port

- Eureka dashboard: http://localhost:8761
- Eureka API base: http://localhost:8761/eureka/

## Start

From the project directory:

```bash
./mvnw spring-boot:run
```

or, with Maven installed:

```bash
mvn spring-boot:run
```

## Existing services to register

- auth-service : 8081
- user-service : 8082
- account-service : 8083
- transaction-service : 8084
- notification-service : 8085
- api-gateway : 8080

Each client should configure:

```yaml
eureka:
  client:
    service-url:
      defaultZone: http://localhost:8761/eureka/
```

and its own `spring.application.name`.
