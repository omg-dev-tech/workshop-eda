# Architecture

## System Architecture

```mermaid
flowchart LR
  Client([Client])
  AGW[API Gateway]
  ORD[Order Service]
  INV[Inventory Service]
  PAYSIM[Payment Simulator]
  FUL[Fulfillment Service]
  ANA[Analytics Service]
  NOT[Notification Service]
  KAFKA[(Kafka)]
  PG1[(Postgres orders)]
  PG2[(Postgres inventory)]
  PG3[(Postgres fulfillment)]
  PG4[(Postgres analytics)]
  PG5[(Postgres notifications)]
  INSTANA[Instana Agent]

  Client --> AGW
  AGW --> ORD
  ORD --- PG1
  INV --- PG2
  FUL --- PG3
  ANA --- PG4
  NOT --- PG5

  ORD <-->|order events| KAFKA
  INV <-->|inventory events| KAFKA
  FUL <-->|fulfillment events| KAFKA
  ANA -->|consume all events| KAFKA
  NOT -->|consume all events| KAFKA

  ORD -->|HTTP /authorize| PAYSIM
  ORD -->|TCP 9091 전문| PAYSIM

  AGW -.->|OTLP| INSTANA
  ORD -.->|OTLP| INSTANA
  INV -.->|OTLP| INSTANA
  FUL -.->|OTLP| INSTANA
  PAYSIM -.->|OTLP| INSTANA
  ANA -.->|OTLP + Metrics| INSTANA
  NOT -.->|OTLP| INSTANA
```

## Event Flow Sequence

```mermaid
sequenceDiagram
  participant C as Client
  participant G as API Gateway
  participant O as Order Service
  participant K as Kafka
  participant I as Inventory Service
  participant P as Payment Simulator
  participant F as Fulfillment Service
  participant A as Analytics Service
  participant N as Notification Service
  participant INS as Instana

  Note over C,INS: 1. Order Creation Flow
  C->>G: POST /orders
  G->>O: createOrder
  O->>O: DB insert PENDING
  O->>INS: OTLP Trace + Metrics
  O->>K: order.v1.created
  K->>I: consume created
  K->>A: consume created
  K->>N: consume created
  N->>N: Send notification
  N->>INS: notification.sent metric

  Note over C,INS: 2. Inventory Check Flow
  I->>I: check and reserve stock
  I->>INS: inventory.reserved metric
  I->>K: inventory.v1.reserved
  K->>O: consume reserved
  K->>A: consume reserved
  K->>N: consume reserved

  Note over C,INS: 3. Payment Flow
  O->>O: ServiceNode taskA, taskB, taskC
  
  alt HTTP Payment
    O->>P: POST /payments/authorize
    P-->>O: APPROVED
  else TCP Legacy Payment
    O->>P: TCP Socket 전문 전송
    P-->>O: 승인응답 전문
  end
  
  P->>INS: payment.authorized metric
  O->>K: order.v1.payment_authorized
  K->>F: consume payment_authorized
  K->>A: consume payment_authorized
  K->>N: consume payment_authorized

  Note over C,INS: 4. Fulfillment Flow
  F->>F: plan and schedule shipment
  F->>INS: fulfillment.scheduled metric
  F->>K: fulfillment.v1.scheduled
  K->>O: consume scheduled
  K->>A: consume scheduled
  K->>N: consume scheduled
  O->>O: state=COMPLETED
  O->>INS: order.completed metric

  Note over C,INS: 5. Analytics and Monitoring
  A->>A: Aggregate metrics
  A->>INS: Business metrics
  INS->>INS: Dependency Map + Traces
```

## Architecture Overview

### Services
- **API Gateway**: Entry point for all client requests
- **Order Service**: Manages order lifecycle and orchestration
- **Inventory Service**: Handles stock management and reservations
- **Payment Simulator**: Simulates external payment gateway (HTTP REST + TCP Socket)
- **Fulfillment Service**: Manages shipping and delivery
- **Analytics Service**: Collects and analyzes business metrics
- **Notification Service**: Sends notifications via email/SMS/push

### Infrastructure
- **Kafka**: Event streaming platform for async communication
- **PostgreSQL**: Database for each service with separate schemas
- **Instana**: APM and observability platform

### Communication Protocols
- **HTTP/REST**: API Gateway ↔ Services, Order ↔ Payment (modern)
- **Kafka Events**: Async event-driven communication between services
- **TCP Socket**: Order ↔ Payment (legacy protocol simulation)

### Key Features
1. **Event-Driven Architecture**: All services communicate via Kafka events
2. **Multi-Protocol Support**: HTTP/REST, Kafka, TCP Socket
3. **OpenTelemetry Integration**: Full distributed tracing and metrics
4. **Virtual Service Nodes**: Method-level tracing with @ServiceNode annotation
5. **Business Metrics**: Custom metrics for business KPIs
6. **Legacy Integration**: TCP socket-based legacy payment gateway simulation
7. **Multi-environment**: Docker Compose for local, Kubernetes for production