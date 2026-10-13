# OBS-001 — Observability Baseline

## Signals

### RED

- Request rate
- Error rate
- Duration (p50/p95/p99)

### Kafka

- consumer processing rate
- consumer lag
- retry/DLT activity

### Outbox

- pending
- processing
- failed
- published
- publish latency

### Database

- Hikari active/idle/pending connections
- transaction/query latency
- PostgreSQL CPU and locks when available

### JVM

- heap used
- GC
- CPU

## Procedure

1. Start Prometheus/Grafana/OTel Collector.
2. Start Payment and Accounting.
3. Generate a controlled payment workload.
4. Watch the outbox backlog while varying polling interval/batch size/concurrency.
5. Watch Accounting consumption and journal creation.
6. Capture the same workload parameters for every run.

## Rule

Do not use dashboard screenshots as performance proof by themselves. Record the workload, duration, concurrency, application version, database state, Kafka state, and exported measurements.
