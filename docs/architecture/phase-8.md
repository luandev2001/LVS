# Phase 8 — Accounting + Observability

## Goal

Phase 8 makes the Kafka event flow produce a real business effect and makes the resulting distributed workflow observable.

```text
Payment Service
  PostgreSQL transaction
      |
      +-- payment
      +-- payment_outbox
               |
               | Outbox Publisher
               v
          Kafka payment.events.v1
               |
               v
        Accounting Consumer
               |
               +-- processed_payment_events
               +-- journal_entry
               +-- journal_line
```

The source requirements for this phase are the simulation-lab rules: use a realistic double-entry model, enforce `SUM(DEBIT) = SUM(CREDIT)`, demonstrate duplicate transaction handling and reversal/compensation, and make experiments observable with metrics, tracing, logs, DB/Kafka/JVM signals.

## Accounting model

Accounts seeded for the simulation:

- `CASH`
- `PAYMENT_REVENUE`
- `REFUND_EXPENSE`

A successful payment creates:

```text
DEBIT  CASH             amount
CREDIT PAYMENT_REVENUE  amount
```

A refund creates the reverse economic entry:

```text
DEBIT  REFUND_EXPENSE   amount
CREDIT CASH             amount
```

The implementation validates the debit/credit totals before persistence. `journal_entry.source_event_id` is unique and `processed_payment_events.event_id` is the consumer idempotency boundary.

## Transaction boundary

Accounting consumes Kafka independently from Payment. There is no distributed transaction between Kafka and the accounting database.

```text
Kafka delivery
   |
   v
Accounting DB transaction
   +-- create journal
   +-- record processed event
   |
   +-- COMMIT
   |
   v
Kafka offset acknowledgement
```

If DB commit succeeds and acknowledgement fails, Kafka can redeliver. The event ID makes the redelivery harmless.

## Observability

### Metrics

Actuator + Prometheus expose:

- HTTP request metrics
- JVM memory / GC metrics
- Hikari connection pool metrics
- custom Payment Outbox gauges: pending, processing, failed, published
- Accounting event received / duplicate / rejected counters
- Accounting journal creation counter
- Accounting event processing timer

Kafka client/container metrics and consumer lag remain an explicit experiment target; do not infer lag from application throughput.

### Tracing

Both Payment and Accounting use Micrometer tracing with the OpenTelemetry bridge. The local Docker Compose stack contains an OpenTelemetry Collector receiving OTLP HTTP traces.

The collector currently uses a debug exporter so the experiment can verify that spans leave the services without introducing a production trace-storage dependency prematurely.

### Structured logging

Both services use ECS structured console logging. Correlation/trace fields can therefore be correlated with application logs when tracing is active.

## Failure scenarios

1. Kafka redelivery after accounting DB commit.
2. Duplicate payment event with the same `eventId`.
3. Duplicate accounting command with a different event ID is intentionally not treated as the same event; the business operation must define its own idempotency key if needed.
4. Accounting validation rejects an unbalanced journal.
5. Consumer processing exception causes retry/DLT behavior.
6. Outbox remains `PROCESSING` after a publisher crash and is later reclaimed.

## Consistency model

Payment state is strongly consistent inside Payment PostgreSQL. Payment-to-Accounting propagation is eventually consistent.

The system deliberately does **not** claim exactly-once end-to-end processing.

The effective safety model is:

```text
Transactional Outbox
+ at-least-once Kafka delivery
+ consumer idempotency
+ database constraints
= duplicate-safe business processing
```

## Measurement plan

All results remain `[TO BE MEASURED]` until the services and infrastructure are actually executed.

| Experiment | Measure |
|---|---|
| Payment → Accounting propagation | end-to-end event latency |
| Outbox pressure | pending/processing/failed counts |
| Publisher tuning | publish throughput and backlog drain time |
| Kafka consumer | processing rate and consumer lag |
| Accounting DB | journal insert latency, DB connections |
| Duplicate delivery | journal count before/after duplicate |
| JVM | memory, GC, CPU |
| HTTP | p50/p95/p99 latency, throughput, errors |

## Run locally

Start infrastructure:

```bash
docker compose up -d
```

Run Payment:

```bash
mvn -pl services/payment-service spring-boot:run
```

Run Accounting:

```bash
mvn -pl services/accounting-service spring-boot:run
```

Prometheus: `http://localhost:9090`
Grafana: `http://localhost:3000`

Create a successful payment:

```bash
curl -X POST 'http://localhost:8084/api/payments?orderReference=ORDER-1&amount=100.00&currency=USD&mode=SUCCESS' \
  -H 'Idempotency-Key: phase8-order-1'
```

Inspect accounting journal entries:

```bash
curl 'http://localhost:8085/api/accounting/journal-entries'
```

Inspect a monthly summary:

```bash
curl 'http://localhost:8085/api/accounting/monthly-summary?month=2026-09'
```

Inspect Payment Outbox:

```bash
curl 'http://localhost:8084/api/outbox/stats'
```
