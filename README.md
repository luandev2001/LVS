# Enterprise Backend Simulation Lab

This repository follows a deliberate BAD -> MEASURE -> ROOT CAUSE -> OPTIMIZE -> MEASURE AGAIN workflow.

## Completed
- Phase 0: engineering foundation
- Phase 1: inventory baseline
- Phase 2: database optimization experiments
- Phase 3: concurrency experiments
- Phase 4: payment gateway simulation
- Phase 5: idempotency
- Phase 6: Kafka
- Phase 7: transactional outbox

## Phase 2
- N+1 baseline retained
- JOIN FETCH optimized endpoint
- composite indexes
- OFFSET pagination baseline
- keyset pagination endpoint
- k6 workload scripts

## Phase 3
- no-lock reservation
- optimistic locking with JPA `@Version`
- pessimistic locking with `PESSIMISTIC_WRITE`
- atomic conditional SQL update

## Important
No performance metrics are fabricated. Benchmark fields remain `[TO BE MEASURED]` until the system is actually run and measured.

## Run examples

```bash
mvn clean verify

docker compose up -d postgres-inventory

# after the service is running
k6 run load-tests/inventory-phase2.js
k6 run load-tests/inventory-keyset.js
STRATEGY=atomic INVENTORY_ID=1 k6 run load-tests/inventory-concurrency.js
```

## Phase 4 — Payment Gateway Simulation

Payment service is now implemented as a failure-injection lab rather than a CRUD endpoint.

Run locally after PostgreSQL is available on port 5436:

```bash
mvn -pl services/payment-service spring-boot:run
```

Create a payment:

```bash
curl -X POST 'http://localhost:8084/api/payments?orderReference=ORDER-1&amount=100&currency=VND&mode=SUCCESS' \
  -H 'Idempotency-Key: demo-1'
```

Failure modes: `SUCCESS`, `FAILURE`, `TIMEOUT`, `DELAYED_SUCCESS`, `DUPLICATE_SUCCESS`, `INVALID_SIGNATURE`.

Experiments:

- `docs/experiments/PAY-001-state-machine.md`
- `docs/experiments/PAY-002-timeout-retry.md`
- `docs/experiments/PAY-003-callback-security.md`

The implementation deliberately does not claim measured performance. Results remain `[TO BE MEASURED]` until the application is run with PostgreSQL and k6.

## Phase 5 — Idempotency

Payment creation now supports durable idempotency across application instances:

- `Idempotency-Key` persisted with a PostgreSQL UNIQUE constraint
- request fingerprint prevents reuse of one key for different business parameters
- concurrent insert race is resolved by the database constraint and winner reload
- duplicate webhook processing uses a database row lock plus domain state validation
- duplicate delivery returns an idempotent outcome instead of applying the business transition twice

Experiments:

- `docs/experiments/IDEMP-001-payment-request.md`
- `docs/experiments/IDEMP-002-webhook-delivery.md`
- `docs/adr/ADR-0005-idempotency.md`

Load tests:

```bash
k6 run load-tests/payment-idempotency.js
```

For the webhook experiment, first create a payment and obtain its gateway reference/signature from a controlled test setup, then use `load-tests/payment-webhook-duplicate.js`.

No performance metrics are fabricated. Results remain `[TO BE MEASURED]`.


## Phase 6 — Kafka

Kafka is now available through `payment.events.v1`. Payment events use `paymentId` as the key. The consumer persists processed event IDs in PostgreSQL to demonstrate application-level idempotency. Kafka producer idempotence is enabled, and a retry/DLT experiment is included.

## Phase 7 — Transactional Outbox

Phase 7 replaces the producer-side direct Kafka publication path with a PostgreSQL transactional outbox.

Implemented:

- `payment_outbox` persisted in the same transaction as payment state changes
- `PENDING -> PROCESSING -> PUBLISHED/FAILED` lifecycle
- `FOR UPDATE SKIP LOCKED` batch claiming
- stale `PROCESSING` recovery
- configurable polling interval
- configurable batch size
- configurable publisher concurrency
- durable retry after Kafka failures
- duplicate publication injection
- consumer-side event-idempotency retained from Phase 6
- outbox statistics endpoint

Useful endpoints:

```bash
curl http://localhost:8084/api/outbox/stats
curl -X POST http://localhost:8084/api/outbox/recover-stale
```

Failure experiment:

```bash
OUTBOX_SIMULATE_CRASH_AFTER_SEND=true \
  mvn -pl services/payment-service spring-boot:run
```

The crash simulation deliberately creates the window:

```text
Kafka send succeeds
        -> publisher fails
        -> outbox remains PROCESSING
        -> stale recovery
        -> same event is published again
        -> idempotent consumer ignores duplicate business effect
```

Experiments:

- `docs/experiments/OUTBOX-001-atomic-db-and-event-intent.md`
- `docs/experiments/OUTBOX-002-crash-after-kafka-send.md`
- `docs/experiments/OUTBOX-003-batch-polling-concurrency.md`
- `docs/adr/ADR-0007-transactional-outbox.md`

Load test:

```bash
k6 run load-tests/payment-outbox.js
```

No benchmark numbers are fabricated. Results remain `[TO BE MEASURED]` until the infrastructure is actually run.

## Phase 8 — Accounting + Observability

Phase 8 introduces a real downstream business effect for the payment events and adds a local observability stack.

Accounting implements:

- Account
- JournalEntry
- JournalLine
- double-entry validation (`SUM(DEBIT) = SUM(CREDIT)`)
- settlement journal
- refund/compensating journal
- processed-event idempotency
- monthly accounting summary

Observability implements:

- Prometheus metrics
- Grafana dashboard provisioning
- Micrometer tracing bridge / OpenTelemetry export
- OpenTelemetry Collector
- ECS structured logging
- Outbox backlog gauges
- Accounting Kafka processing/journal metrics
- Actuator/JVM/Hikari metrics

Useful endpoints:

```bash
curl http://localhost:8085/api/accounting/journal-entries
curl 'http://localhost:8085/api/accounting/monthly-summary?month=2026-09'
curl http://localhost:8084/api/outbox/stats
```

Experiments:

- `docs/experiments/ACCOUNTING-001-double-entry.md`
- `docs/experiments/ACCOUNTING-002-idempotent-consumer.md`
- `docs/experiments/OBS-001-observability-baseline.md`
- `docs/experiments/OBS-002-outbox-and-accounting-latency.md`

Load test:

```bash
k6 run load-tests/accounting-phase8.js
```

No performance numbers are fabricated. Results remain `[TO BE MEASURED]` until the infrastructure is actually executed.

## Phase 9 — Saga choreography
Payment → Accounting → Inventory is a complete event-driven Saga with local outboxes, compensation, timeout, duplicate protection and observability hooks. See `docs/architecture/phase-9.md`.

## Phase 10 — Redis Cache & Failure Scenarios

Phase 10 adds Redis to the inventory read path. The implementation intentionally exposes the mechanics rather than hiding them behind a cache annotation:

- cache-aside hit/miss
- TTL
- versioned warehouse invalidation
- after-commit cache invalidation
- stale-data reproduction when invalidation is intentionally skipped
- cache stampede reproduction with `NAIVE` mode
- Redis single-flight lock with `LOCKED` mode
- Redis failure with PostgreSQL fallback

Run Redis with:

```bash
docker compose up -d redis inventory-db
```

Run the inventory service normally, then try:

```text
GET /api/inventory/cached?warehouseCode=WH-1&page=0&size=50&protection=LOCKED
POST /api/inventory/1/reserve/atomic?amount=1&invalidateCache=true
POST /api/inventory/1/reserve/atomic?amount=1&invalidateCache=false
```

For stampede experiments, set:

```text
INVENTORY_CACHE_TTL_SECONDS=3
INVENTORY_CACHE_SIMULATED_DB_DELAY_MS=1000
```

Then compare:

```bash
PROTECTION=NAIVE SCENARIO=stampede k6 run load-tests/cache-phase10.js
PROTECTION=LOCKED SCENARIO=stampede k6 run load-tests/cache-phase10.js
```

Do not treat any benchmark result as established until the experiment is actually executed. See `docs/experiments/CACHE-001-cache-aside.md` through `CACHE-004-redis-failure.md`.

## Current phase

Phase 11 adds `CompletableFuture` fan-out/fan-in, a bounded async executor, explicit HTTP 429 backpressure, and experiments comparing application thread concurrency with the Hikari/PostgreSQL connection pool. See `docs/architecture/phase-11.md`.

Phase 12 adds configurable Kafka listener concurrency/poll guardrails plus a resilience lab for bounded retry, circuit breaker, and bulkhead. See `docs/architecture/phase-12.md`.


## Phase 13 — End-to-end Observability

The lab now provisions Prometheus, Grafana, OpenTelemetry Collector, and Tempo. Inventory, payment, and accounting expose Prometheus metrics; tracing is configured over OTLP; Kafka observation is enabled where supported; structured ECS logs are enabled for business services.

```bash
docker compose up -d
k6 run load-tests/observability-phase13.js
```

Open Prometheus at `http://localhost:9090`, Grafana at `http://localhost:3000`, and Tempo query API at `http://localhost:3200`. Check Prometheus targets before trusting dashboard gaps. Full tracing defaults to 100% for lab visibility; tune `TRACING_SAMPLE_PROBABILITY` for experiments and never use high-cardinality business identifiers as metric labels. See `docs/architecture/phase-13.md` and OBS-004/OBS-005.

## Phase 14 — Full Load Test

Phase 14 adds reproducible k6 smoke, component, mixed and stress profiles, a run matrix, result capture guidance, Prometheus alert rules, and bottleneck-isolation experiments. It deliberately contains no fabricated benchmark results. See `docs/architecture/phase-14.md`, `docs/experiments/LOAD-001-full-system-baseline.md`, and `docs/experiments/LOAD-002-bottleneck-and-optimization.md`.
