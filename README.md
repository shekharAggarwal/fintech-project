# 🏦 FinTech Microservices Platform

[![Java](https://img.shields.io/badge/Java-21-orange.svg)](https://www.oracle.com/java/)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.2-green.svg)](https://spring.io/projects/spring-boot)
[![Docker](https://img.shields.io/badge/Docker-Compose-blue.svg)](https://www.docker.com/)
[![License](https://img.shields.io/badge/License-MIT-yellow.svg)](LICENSE)

A robust, N-Tier microservices architecture for a payment gateway system. Designed for high availability, scalability, and security, utilizing industry-standard patterns and tools.

---

## 🏗️ System Architecture

The system follows a reactive microservices architecture. The **API Gateway** acts as the single entry point, routing requests to specific services. Inter-service communication is primarily **Event-Driven** via a message broker to ensure loose coupling and high availability.

### High-Level Components

```mermaid
graph TD
    %% Clients
    Client([📱 Client / Web]) -->|HTTPS| Gateway[🛡️ API Gateway]

    %% Infrastructure
    subgraph "Infrastructure"
        Config[Config Server]
        Discovery[Discovery Service]
        AuthDB[(Auth DB)]
        MainDB[(Main Sharded DB)]
        Broker{Message Broker}
        Redis[(Redis Cache)]
    end

    %% Services
    subgraph "Core Business Services"
        Gateway -->|/auth| Auth[Auth Service]
        Gateway -->|/payments| Payment[Payment Service]
        Gateway -->|/transaction| Transaction[Transaction Service]
        Gateway -->|/user| User[User Service]
    end

    subgraph "Support Services"
        Ledger[Ledger Service]
        Notification[Notification Service]
        Reporting[Reporting Service]
        Scheduler[Scheduler Service]
    end

    %% Data Flow
    Auth --> AuthDB
    Payment --> Redis
    Transaction --> MainDB
    User --> MainDB
    
    %% Event Flow
    Payment -->|Publish Events| Broker
    Transaction -->|Publish Events| Broker
    
    Broker -->|Consume| Transaction
    Broker -->|Consume| Payment
    Broker -->|Consume| Ledger
    Broker -->|Consume| Notification
    Broker -->|Consume| Reporting
```

---

## 🔄 Transaction Flow

The transaction process is **asynchronous** and **event-driven**, ensuring that long-running processes do not block the user experience.

### 1. Payment Initiation & OTP Verification (Happy Path)

```mermaid
sequenceDiagram
    autonumber
    actor User
    participant Gateway
    participant Payment as Payment Service
    participant Redis
    participant Broker as Message Broker (Kafka/RabbitMQ)
    participant Txn as Transaction Service
    participant Notify as Notification Service

    %% Step 1: Initiate
    User->>Gateway: POST /api/payments/transfer
    Gateway->>Payment: Initiate Payment Request
    Payment->>Payment: Create Record (PENDING_VERIFICATION)
    Payment->>Redis: Generate & Store OTP
    Payment->>Broker: Publish OtpNotificationEvent
    Payment-->>User: 200 OK (Payment ID Created)
    
    %% Async Notification
    Broker->>Notify: Consume OtpNotificationEvent
    Notify->>User: Send OTP (SMS/Email)

    %% Step 2: Verify OTP
    User->>Gateway: POST /api/payments/{id}/verify-otp
    Gateway->>Payment: Verify OTP Request
    Payment->>Redis: Validate OTP
    
    alt OTP Valid
        Payment->>Payment: Update Status (AUTHORIZED)
        Payment->>Broker: Publish PaymentInitiatedEvent
        Payment-->>User: 200 OK (Verification Success)
        
        %% Step 3: Transaction Processing
        Broker->>Txn: Consume PaymentInitiatedEvent
        Txn->>Txn: Idempotency Check
        Txn->>Txn: Execute Debit/Credit (Bank Adapter)
        
        alt Success
            Txn->>Txn: Status = COMPLETED
            Txn->>Broker: Publish TransactionCompletedEvent
        else Failure
            Txn->>Txn: Status = FAILED
            Txn->>Broker: Publish TransactionCompletedEvent
        end
        
        %% Step 4: Finalize
        Broker->>Payment: Consume TransactionCompletedEvent
        Payment->>Payment: Update Final Status
        
        Broker->>Notify: Consume TransactionCompletedEvent
        Notify->>User: Send Success/Failure Notification
        
    else OTP Invalid
        Payment->>Payment: Status = FAILED
        Payment-->>User: 400 Bad Request
    end
```

---

## 🚀 Services Overview

| Service | Port | Description |
| :--- | :--- | :--- |
| **Gateway Service** | `8080` | Entry point, routing, load balancing, and security filtering. |
| **Auth Service** | `8081` | User authentication, token generation (JWT), and authorization logic. |
| **User Service** | `8093` | Manages user profiles, KYC data, and preferences. |
| **Transaction Service** | `8086` | Core transaction processing logic. |
| **Payment Service** | `8084` | Integrates with external payment providers/gateways. |
| **Ledger Service** | `8087` | Double-entry bookkeeping and accounting records. |
| **Notification Service** | `8083` | Sends emails, SMS, and push notifications via events. |
| **Reporting Service** | `8092` | Generates analytics and financial reports. |
| **Scheduler Service** | `8089` | Manages recurring tasks and jobs. |
| **Retry Service** | `8090` | Handles failed transactions and retry policies. |
| **Config Server** | `8888` | Centralized configuration management. |

---

## 🛠️ Getting Started

### Prerequisites
- **Docker Desktop** (with Docker Compose v2.0+)
- **Java 21** (for local development/IDE)
- **Git**

### 🏁 Running the Platform

We provide a helper script `start.sh` in the `infra` directory to manage the lifecycle of the application.

1. **Clone the repository:**
   ```bash
   git clone <repository-url>
   cd fintech-project
   ```

2. **Start the Infrastructure and Services:**
   Navigate to the `infra` folder and run the start script.
   ```bash
   cd infra
   ./start.sh build
   ```
   *Note: The `build` argument forces a rebuild of the Docker images. Omit it to use existing images.*

3. **Verify Status:**
   Check the running containers:
   ```bash
   docker compose ps
   ```

### 🛑 Stopping the Platform
To gracefully stop all services and remove containers:
```bash
cd infra
./stop.sh
```

---

## 📊 Observability & Monitoring Dashboard

Once the stack is up and running, you can access the following dashboards:

| Tool | URL | Credentials (Default) |
| :--- | :--- | :--- |
| **Grafana** | [http://localhost:3000](http://localhost:3000) | `admin` / `admin` |
| **Prometheus** | [http://localhost:9090](http://localhost:9090) | N/A |
| **Jaeger UI** | [http://localhost:16686](http://localhost:16686) | N/A |
| **Splunk** | [http://localhost:8000](http://localhost:8000) | `admin` / `splunkpassword123` |
| **RabbitMQ** | [http://localhost:15672](http://localhost:15672) | `guest` / `guest` |

---

## 🔐 Security & Certificates

The project uses a Public Key Infrastructure (PKI) setup for secure communications.
- **Location**: `/certs` directory.
- **Files**:
  - `fintech.p12`: Server keystore for SSL.
  - `fintech-truststore.jks`: Truststore for verifying internal certificates.
  - `jwt-keystore.p12`: Keypair for signing JWTs.
  - `jwt-public.crt`: Public key for verifying JWT signatures.

> **Note:** For development, self-signed certificates are included. For production, ensure these are replaced with CA-signed certificates.

---

## 👨‍💻 Development

To run an individual service locally (e.g., inside IntelliJ IDEA):

1. **Ensure Infrastructure is Up**:
   Run `./start.sh infra` to start only databases, brokers, and config server.
   ```bash
   cd infra
   ./start.sh infra
   ```
2. **Configure Environment Variables**:
   Set the required env vars in your IDE run configuration (refer to `infra/.env` for values like `POSTGRES_USER`, `POSTGRES_PASSWORD`).
3. **Run the Application**:
   Start the `main` class of the desired service.

---

## 📄 License
This project is licensed under the MIT License.
