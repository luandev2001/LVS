# OBS-002 — Outbox to Accounting Latency

## Goal

Measure the end-to-end asynchronous propagation path:

`HTTP payment request -> DB commit -> outbox publication -> Kafka -> Accounting journal commit`

## Suggested dimensions

- polling interval: 100 / 500 / 1000 ms
- batch size: 10 / 50 / 200
- publisher concurrency: 1 / 2 / 4
- controlled payment rate

These are experiment inputs, not performance claims.

## Measurements

- HTTP latency
- outbox pending depth
- time from event creation to journal creation
- Kafka consumer lag
- Accounting processing latency
- DB connection utilization
- JVM CPU/memory

All final values: `[TO BE MEASURED]`.
