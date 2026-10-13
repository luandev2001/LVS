# Phase 0 — Engineering Foundation

## Objective

Create a reproducible repository and service boundary without prematurely implementing the optimized architecture.

## Repository topology

```text
enterprise-backend-simulation-lab/
├── pom.xml
├── docker-compose.yml
├── docs/
│   ├── architecture/
│   ├── adr/
│   └── experiments/
└── services/
    ├── inventory-service/
    ├── facility-service/
    ├── accounting-service/
    └── payment-service/
```

## Service ownership

| Service | Responsibility | Database |
|---|---|---|
| inventory-service | Product/stock/inventory | inventory_db |
| facility-service | Facility/warehouse/location | facility_db |
| accounting-service | Accounts/journals | accounting_db |
| payment-service | Payment lifecycle/gateway integration | payment_db |

## Deliberate decisions

### Separate databases

Each service owns its persistence boundary. Cross-service consistency will later be implemented with messaging, Outbox and Saga experiments rather than shared-table transactions.

### No shared common business module

Phase 0 does not create a `common-domain` module. Shared business models create coupling between service boundaries. Shared technical utilities can be introduced later only when a concrete duplication problem is measured.

### No Kafka yet

Kafka is intentionally deferred until the synchronous baseline exists. Otherwise we lose the ability to compare synchronous and asynchronous behavior.

### No Redis yet

Redis is intentionally deferred until a measured read/query bottleneck gives us a concrete caching experiment.

### No Event Sourcing yet

The first inventory implementation will be state-based CRUD. Event Sourcing will be introduced only after the CRUD baseline is observable.

## Phase 0 evidence

Record:

- JDK version
- Maven version
- Docker version
- Spring Boot version
- Git commit hash
- OS/CPU/memory information

No performance metric is recorded in Phase 0.
