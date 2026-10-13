# LOAD-001 — Full-system baseline

## Hypothesis

A reproducible mixed workload will expose the first saturated resource more clearly than independent endpoint tests.

## Procedure

1. Run smoke test and ensure all services are healthy.
2. Keep code, seed data, pools, cache TTL, Kafka partitions, and tracing settings fixed.
3. Run `MODE=MIXED TARGET_VUS=10 STEADY=3m` then `TARGET_VUS=25` and `TARGET_VUS=50`.
4. Save k6 summaries and Prometheus/trace evidence.
5. Repeat each profile at least three times after a warm-up run.

## Record

| Profile | VUs | RPS | p50 | p95 | p99 | Error rate | DB pool pending | Kafka lag | Notes |
|---|---:|---:|---:|---:|---:|---:|---:|---:|---|
| Warm-up | [TO BE MEASURED] | [TO BE MEASURED] | [TO BE MEASURED] | [TO BE MEASURED] | [TO BE MEASURED] | [TO BE MEASURED] | [TO BE MEASURED] | [TO BE MEASURED] | |
| Baseline | 10 | [TO BE MEASURED] | [TO BE MEASURED] | [TO BE MEASURED] | [TO BE MEASURED] | [TO BE MEASURED] | [TO BE MEASURED] | [TO BE MEASURED] | |
| Medium | 25 | [TO BE MEASURED] | [TO BE MEASURED] | [TO BE MEASURED] | [TO BE MEASURED] | [TO BE MEASURED] | [TO BE MEASURED] | [TO BE MEASURED] | |
| Stress | 50 | [TO BE MEASURED] | [TO BE MEASURED] | [TO BE MEASURED] | [TO BE MEASURED] | [TO BE MEASURED] | [TO BE MEASURED] | [TO BE MEASURED] | |

## Interpretation

Do not declare an optimization successful from average latency alone. Compare tail latency, errors, throughput, saturation indicators, and business correctness. These values must come from actual runs.
