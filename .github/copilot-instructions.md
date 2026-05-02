# Copilot Instructions — FinTech Microservices Platform

## Architecture Overview

This is an event-driven microservices payment gateway built with **Java 17**, **Spring Boot 3.4**, and **Gradle** (version catalog). Each service is an independent Gradle project under `services/`, sharing a common version catalog at `services/gradle/libs.versions.toml`.

### Service Communication

- **Synchronous**: API Gateway (`gateway-service` on port 8080) routes HTTPS requests to downstream services via Spring Cloud Gateway.
- **Asynchronous**: Services communicate through **Kafka** (transaction events) and **RabbitMQ** (notifications, user creation). Both brokers are used simultaneously—Kafka for high-throughput payment/transaction flows, RabbitMQ for notification/session/user events.

### Key Services & Their Roles

| Service | Responsibility |
|---------|---------------|
| `gateway-service` | Entry point, JWT validation, rate limiting (Bucket4j + Redis), circuit breaking |
| `auth-service` | User registration/login, JWT issuance (RS256 via keystore) |
| `authorization-service` | RBAC + field-level access control, Flyway migrations |
| `payment-service` | Payment initiation, OTP verification via Redis, publishes to Kafka |
| `transaction-service` | Processes payments via Bank Adapter pattern, idempotency checks |
| `ledger-service` | Double-entry bookkeeping |
| `user-service` | User profile and KYC management |
| `config-server` | Spring Cloud Config serving from `services/config-repo/` |

### Shared Module

`module/security` is a shared Java library included via Gradle's `include(":security")` in each service's `settings.gradle`. It provides:
- `@RequireAuthorization` — AOP-based method-level RBAC (roles, resourceType, validateArgs)
- `@FilterResponse` / `@FieldAccessControl` — Response field filtering by role
- `AuthorizationFilter` — Servlet filter that calls the authorization-service
- `AuthorizationContextHolder` — Thread-local context for current user's permissions

Services reference it as: `implementation project(":security")` with the project directory mapped to `../../module/security`.

## Build & Run Commands

Each service has its own `gradlew`. Run from within the service directory:

```bash
# Build a single service
cd services/payment-service
./gradlew bootJar

# Run tests for a single service
cd services/payment-service
./gradlew test

# Run a specific test class
./gradlew test --tests "com.fintech.paymentservice.SomeTest"

# Run a specific test method
./gradlew test --tests "com.fintech.paymentservice.SomeTest.methodName"
```

### Docker (full platform)

```bash
cd infra
./start.sh build    # Build images and start everything
./start.sh infra    # Start only infrastructure (DBs, brokers, monitoring)
./start.sh          # Start with existing images
./stop.sh           # Tear down
```

## Key Conventions

### ID Generation
All services use **Snowflake ID generators** (`util/SnowflakeIdGenerator.java`) for distributed unique IDs. Each service has a unique `snowflake.node-id` in its properties.

### Package Structure
Every service follows: `com.fintech.<servicename>/` with sub-packages:
- `controller/` — REST endpoints
- `service/` — Business logic
- `entity/` — JPA entities
- `repository/` — Spring Data repos
- `dto/` — Request/response objects
- `messaging/` — Kafka/RabbitMQ publishers and listeners
- `config/` — Spring configuration classes
- `util/` — Utilities (Snowflake, etc.)
- `model/` — Domain models (non-entity)

The transaction-service additionally has `adapter/` (Bank Adapter pattern with `BankAdapter` interface and impls like `HdfcBankAdapter`, `SelfBankAdapter`).

### Configuration

- **Profiles**: `application.properties` (base), `application-dev.properties`, `application-prod.properties`
- **Centralized config**: `services/config-repo/prod/application-prod.yml` holds shared prod settings pulled by Spring Cloud Config
- **Environment variables**: Defined in `infra/.env`, referenced in compose and config files

### Database Strategy

- **PostgreSQL** with multiple dedicated databases: `fintech_main`, `fintech_auth`, `fintech_scheduler`, `fintech_retry`
- **Sharding**: Main DB is sharded (3 shards + read replicas) via ShardingSphere Proxy
- **Migrations**: Flyway (`db/migration/V*__*.sql`) in authorization-service; other services use Hibernate `ddl-auto: update`

### Security

- **mTLS** between services using keystores in `/certs`
- **JWT** signed with RS256 via `jwt-keystore.p12`; public key at `jwt-public.crt`
- Gateway validates JWT, downstream services use the security module's authorization filter

### Observability

- **Tracing**: OpenTelemetry → Jaeger (port 16686)
- **Metrics**: Micrometer → Prometheus (port 9090) → Grafana (port 3000)
- **Logging**: Logstash encoder + Splunk HEC (structured JSON logs)
- Shared logging config in `services/common-logging-config/`

### Resilience Patterns

- **Circuit Breaker**: Resilience4j across all services
- **Rate Limiting**: Bucket4j with Redis backend (gateway)
- **Retry**: Dedicated retry-service for failed transactions

### Dependency Management

Dependencies are declared via the shared **Gradle version catalog** (`services/gradle/libs.versions.toml`). Use `libs.<name>` or `libs.bundles.<name>` references in `build.gradle` files—do not hardcode versions in individual services.
