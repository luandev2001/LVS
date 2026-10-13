# Phase 14 — Full Load Test and Bottleneck Analysis

## Goal

Run controlled end-to-end load against Inventory, Payment, and Accounting, then use Phase 13 telemetry to explain the bottleneck. This phase defines reproducible workloads and evidence capture; it does not contain pre-fabricated benchmark results.

## Workload profiles

- **Smoke**: verify service health endpoints before load.
- **Inventory baseline**: mixed uncached optimized list and Redis-cached list reads.
- **Mixed load**: 70% inventory reads, 20% payment creates, 10% accounting journal reads.
- **Payment load**: idempotent payment creation with a unique key per operation.
- **Stress**: increase VUs to discover saturation; stop if error rate or resource use becomes unsafe.
- **Soak**: manually run a steady workload for 30–60 minutes and watch memory, pool wait, Kafka lag, and outbox backlog.

## Run

1. Build and test first: `mvn clean verify`.
2. Start dependencies: `docker compose up -d`.
3. Start Inventory on 8081, Payment on 8084, and Accounting on 8085 using each service's README/config.
4. Wait until `/actuator/health` returns 200 for all services.
5. Run `k6 run load-tests/phase14-smoke.js`.
6. Run one workload at a time or execute `load-tests/phase14-matrix.sh` from the repository root. Matrix results are saved under `results/phase14/`.

Example manual run:

```bash
MODE=MIXED TARGET_VUS=25 RAMP_UP=30s STEADY=3m RAMP_DOWN=30s \
  k6 run --summary-export results-phase14.json load-tests/full-system-phase14.js
```

Modes: `INVENTORY`, `PAYMENT`, `ACCOUNTING`, `MIXED`. Override service URLs with `INVENTORY_URL`, `PAYMENT_URL`, and `ACCOUNTING_URL`.

## Required evidence per run

Record git commit, machine/CPU/memory, Java version, Docker image versions, DB seed size, service pool settings, Kafka partitions/consumer concurrency, cache TTL, k6 VUs/stages, run duration, and whether tracing sampling was enabled.

Capture:

- k6 request rate, p50/p95/p99, failed-request rate, checks, and per-operation latency;
- Prometheus HTTP rate/error/latency, JVM heap/GC, Hikari active/idle/pending, executor active/queue/rejections, cache hit/miss, and custom business metrics;
- Kafka consumer lag, outbox backlog/oldest event age, processed event counts, and DLQ counts where exposed;
- representative trace IDs for slow and failed requests, plus corresponding structured logs;
- PostgreSQL `pg_stat_activity`, slow-query evidence/`EXPLAIN (ANALYZE, BUFFERS)` where safe.

## Bottleneck diagnosis decision tree

1. **p95 rises, DB pool pending rises** → compare executor concurrency with Hikari maximum pool size; inspect SQL latency and lock waits before raising pool size.
2. **p95 rises, DB pool is healthy, executor queue rises** → inspect blocking calls, task duration, queue/rejection policy and downstream waits.
3. **HTTP is healthy but Kafka lag grows** → consumer throughput is below producer rate; inspect partitions, consumer concurrency, processing time, retries, DB contention, and poison messages.
4. **Outbox backlog grows** → compare event production rate with publisher throughput and broker latency; inspect stale PROCESSING rows and failures.
5. **CPU high and latency rises** → inspect GC, serialization, tracing overhead, hot loops and query plans.
6. **Redis hit rate drops / DB QPS rises** → inspect key churn, TTL, invalidation behavior and stampede protection.
7. **Errors rise after retries** → estimate retry amplification and check circuit-breaker/bulkhead state; avoid retrying non-transient business failures.

## Optimization protocol

For each experiment: establish baseline → change exactly one variable → repeat the same workload and environment → compare confidence/repeatability → retain or revert. Do not change pool size, SQL, cache TTL, and consumer concurrency in one run because attribution becomes impossible.

No benchmark is considered valid unless the actual run output and environment metadata are saved. All result cells remain `[TO BE MEASURED]` until a run has occurred.
