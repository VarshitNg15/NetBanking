# NetBanking Platform: OpenFeign & Inter-Service Communication Guide

This guide establishes the architectural blueprint for synchronous inter-service communication via **Spring Cloud OpenFeign** across the NetBanking microservices ecosystem, drawing inspiration from [TransactSphere](https://github.com/VarshitNg15/TransactSphere).

---

## 1. Architectural Principles: OpenFeign vs. Apache Kafka

The NetBanking platform uses a hybrid communication model:

```
                  ┌───────────────────────┐
                  │   Spring Cloud        │
                  │   API Gateway         │
                  └──────────┬────────────┘
                             │
            ┌────────────────┼────────────────┐
            │ (Edge Route)   │ (Edge Route)   │ (Edge Route)
            ▼                ▼                ▼
     ┌─────────────┐  ┌─────────────┐  ┌─────────────┐
     │ Auth        │  │ User        │  │ Account     │
     │ Service     │  │ Service     │  │ Service     │
     └──────┬──────┘  └──────▲──────┘  └──────▲──────┘
            │ OpenFeign      │                │
            └────────────────┤                │ OpenFeign
                             │ OpenFeign      │ (Debit/Credit/Ledger)
                             │                │
                      ┌──────┴──────┐         │
                      │ Transaction ├─────────┘
                      │ Service     │
                      └──────┬──────┘
                             │ Kafka (Eventual Consistency)
                             ▼
                      ┌─────────────┐
                      │ Notification│
                      │ & Audit     │
                      └─────────────┘
```

### When to Use **OpenFeign (Synchronous REST)**
- **Immediate Consistency Required**: The caller cannot proceed without the immediate response of the callee (e.g., deducting balance from Account Service during a transfer).
- **Precondition & State Validation**: Verifying that a customer is `ACTIVE` before executing a high-value fund transfer.
- **Read Aggregations**: Querying account ledger history from Account Service to compile a statement PDF/CSV inside Transaction Service.

### When to Use **Apache Kafka (Asynchronous Events)**
- **Eventual Consistency**: Side-effects that do not block the user's primary journey (e.g. sending transactional emails, generating welcome notifications).
- **Immutable Audit Logging**: Emitting security and compliance events across all services to `NOTIFICATION_SCHEMA.AUDIT_LOGS`.
- **Decoupled Architecture**: High-latency downstream operations (SMTP provider latency, third-party SMS gateways) will never degrade transfer throughput.

---

## 2. OpenFeign Inter-Service Matrix

| Calling Service | Feign Client Interface | Target Microservice | Target Endpoint | HTTP Method | Purpose |
| :--- | :--- | :--- | :--- | :---: | :--- |
| **Transaction Service** | `AccountServiceClient` | `account-service` (`:8083`) | `/api/accounts/{id}` | `GET` | Validate account existence, `ACTIVE` status, and matching currency. |
| **Transaction Service** | `AccountServiceClient` | `account-service` (`:8083`) | `/api/accounts/{id}/debit` | `POST` | Atomically deduct available balance and record double-entry debit ledger row. |
| **Transaction Service** | `AccountServiceClient` | `account-service` (`:8083`) | `/api/accounts/{id}/credit` | `POST` | Atomically credit available balance and record double-entry credit ledger row. |
| **Transaction Service** | `AccountServiceClient` | `account-service` (`:8083`) | `/api/accounts/{id}/ledger` | `GET` | Retrieve ledger history for statement PDF/CSV compilation. |
| **Transaction Service** | `UserServiceClient` | `user-service` (`:8082`) | `/api/customers/{id}` | `GET` | Verify customer status (`ACTIVE` vs `BLOCKED`/`INACTIVE`) before initiating transfer. |
| **Account Service** | `UserServiceClient` | `user-service` (`:8082`) | `/api/customers/{id}` | `GET` | Verify customer identity & KYC verification before opening an account or approving requests. |
| **Auth Service** | `CustomerServiceClient` | `user-service` (`:8082`) | `/api/customers` | `POST` | Provision the customer profile shell in `USER_SCHEMA` upon user registration (fallback to Kafka event). |
| **Notification Service** | `UserServiceClient` | `user-service` (`:8082`) | `/api/customers/{id}` | `GET` | Synchronous fallback to retrieve recipient email/phone if omitted from Kafka event. |

---

## 3. Client Specifications & Code Implementation

### A. Transaction Service $\rightarrow$ Account Service Client

```java
package com.netbanking.transaction.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;

@FeignClient(name = "account-service", url = "${clients.account-service.url:http://localhost:8083}")
public interface AccountServiceClient {

    @GetMapping("/api/accounts/{accountId}")
    AccountResponse getAccount(@PathVariable("accountId") Long accountId);

    @PostMapping("/api/accounts/{accountId}/debit")
    BalanceOperationResponse debit(
            @PathVariable("accountId") Long accountId,
            @RequestBody BalanceOperationRequest request
    );

    @PostMapping("/api/accounts/{accountId}/credit")
    BalanceOperationResponse credit(
            @PathVariable("accountId") Long accountId,
            @RequestBody BalanceOperationRequest request
    );

    @GetMapping("/api/accounts/{accountId}/ledger")
    List<LedgerEntryResponse> getLedger(
            @PathVariable("accountId") Long accountId,
            @RequestParam("fromDate") String fromDate,
            @RequestParam("toDate") String toDate
    );

    record AccountResponse(Long accountId, String customerId, String accountStatus, String currencyCode) {}
    record BalanceOperationRequest(BigDecimal amount, String transactionReference, String description, String initiatedBy) {}
    record BalanceOperationResponse(boolean successful, String message) {}
    record LedgerEntryResponse(String entryReference, String entryType, BigDecimal amount, String description, String createdAt) {}
}
```

---

### B. Transaction Service $\rightarrow$ User Service Client

```java
package com.netbanking.transaction.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient(name = "user-service", url = "${clients.user-service.url:http://localhost:8082}")
public interface UserServiceClient {

    @GetMapping("/api/customers/{customerId}")
    CustomerResponse getCustomer(@PathVariable("customerId") String customerId);

    record CustomerResponse(String customerId, String customerStatus, String email) {}
}
```

---

### C. Auth Service $\rightarrow$ User Service Client

```java
package com.netbanking.auth.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

@FeignClient(name = "user-service", url = "${clients.user-service.url:http://localhost:8082}")
public interface CustomerServiceClient {

    @PostMapping("/api/customers")
    CustomerResponse createCustomer(@RequestBody CustomerCreateRequest request);

    @GetMapping("/api/customers/{customerId}")
    CustomerResponse getCustomer(@PathVariable("customerId") String customerId);

    record CustomerCreateRequest(String customerId, String email, String customerStatus) {}
    record CustomerResponse(String customerId, String customerStatus) {}
}
```

---

## 4. Security: Token & Edge Header Propagation in Feign

When an authenticated user calls an endpoint through the API Gateway, the Gateway extracts pure JWT claims and injects:
- `X-Customer-Id`
- `X-User-Email`
- `X-User-Roles`

To propagate these security headers and the `Authorization: Bearer <token>` through OpenFeign calls, configure a `RequestInterceptor`:

```java
package com.netbanking.common.feign;

import feign.RequestInterceptor;
import feign.RequestTemplate;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

@Component
public class FeignAuthHeaderInterceptor implements RequestInterceptor {

    @Override
    public void apply(RequestTemplate template) {
        ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attributes != null) {
            HttpServletRequest request = attributes.getRequest();
            String authHeader = request.getHeader("Authorization");
            if (authHeader != null && !authHeader.isBlank()) {
                template.header("Authorization", authHeader);
            }
            String customerId = request.getHeader("X-Customer-Id");
            if (customerId != null) {
                template.header("X-Customer-Id", customerId);
            }
            String roles = request.getHeader("X-User-Roles");
            if (roles != null) {
                template.header("X-User-Roles", roles);
            }
        }
    }
}
```

---

## 5. Resilience & Error Decoding

When a Feign call fails (e.g., Account Service returns HTTP 400 for `Insufficient funds`), a custom `ErrorDecoder` translates the HTTP status into application-specific domain exceptions rather than generic `FeignException`:

```java
package com.netbanking.common.feign;

import feign.Response;
import feign.codec.ErrorDecoder;
import org.springframework.stereotype.Component;

@Component
public class CustomFeignErrorDecoder implements ErrorDecoder {

    private final ErrorDecoder defaultDecoder = new Default();

    @Override
    public Exception decode(String methodKey, Response response) {
        if (response.status() == 400) {
            return new IllegalArgumentException("Target service rejected request (400 Bad Request)");
        }
        if (response.status() == 404) {
            return new IllegalStateException("Target resource not found (404)");
        }
        if (response.status() >= 500) {
            return new RuntimeException("Target microservice is temporarily unavailable (5xx)");
        }
        return defaultDecoder.decode(methodKey, response);
    }
}
```

---

## 6. Comparison with TransactSphere

In [TransactSphere](https://github.com/VarshitNg15/TransactSphere):
- `admin-service` communicates with `user-service`, `account-service`, and `fraud-service` via OpenFeign.
- `transaction-service` communicates with `account-service` and `user-service` via OpenFeign for immediate debit/credit.
- Asynchronous actions (notifications, analytics events, global audit logs) are decoupled via Apache Kafka.

NetBanking follows this exact architectural separation of concerns, ensuring maximum performance, data isolation per service, and zero tight coupling.
