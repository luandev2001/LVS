# LOAD-002 — Bottleneck isolation and re-measurement

## Goal

Use traces, metrics, and logs to identify a limiting resource, change one variable, and verify the outcome under the same workload.

## Candidate experiments

- Hikari pool: vary only `maximum-pool-size`; observe pending borrowers, DB CPU, lock waits and latency. Larger pools can worsen database contention.
- Async executor: vary only worker count/queue capacity; observe queue depth, rejection count, Hikari pending and p99.
- Kafka: vary consumer concurrency only within partition limits; observe lag and downstream DB contention.
- Redis: compare cached/uncached read ratio; observe hit rate, DB QPS and staleness correctness.
- Tracing: compare sampling ratios under the same traffic; observe CPU, allocation, export queue and trace completeness.
- SQL/index: capture `EXPLAIN (ANALYZE, BUFFERS)` and query timing before changing the query or index.

## Result template

- Hypothesis: [TO BE FILLED]
- Single changed variable: [TO BE FILLED]
- Baseline run ID: [TO BE FILLED]
- Optimized run ID: [TO BE FILLED]
- Throughput / p95 / p99 / error rate: [TO BE MEASURED]
- Saturation signal before and after: [TO BE FILLED]
- Correctness checks passed: [TO BE FILLED]
- Decision: KEEP / REVERT / NEED MORE DATA
