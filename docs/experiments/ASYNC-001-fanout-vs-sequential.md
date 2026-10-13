# ASYNC-001 — Fan-Out/Fan-In versus Sequential DB Reads

## Hypothesis

When independent page queries are I/O-bound and sufficient executor/DB capacity exists, concurrent execution can reduce wall-clock latency compared with sequential execution.

## Control

`GET /api/inventory/async/sequential?warehouseCode=WH-1&pages=4&size=50`

## Treatment

`GET /api/inventory/async?warehouseCode=WH-1&pages=4&size=50`

## Variables

- pages
- page size
- async core/max pool
- Hikari maximum pool
- simulated DB delay
- VUs

## Measurements

- p50/p95/p99
- throughput
- Hikari active/pending
- PostgreSQL active sessions
- JVM CPU

## Result

[TO BE MEASURED]
