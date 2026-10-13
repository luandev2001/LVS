# Load tests

Phase 6 adds `kafka-phase6.js`.

```bash
k6 run load-tests/kafka-phase6.js
```

Record actual results only after Kafka + PostgreSQL are running. Do not copy example numbers into experiment documents.

## Phase 12 — resilience lab

```bash
FAIL=true RETRIES=0 VUS=30 k6 run load-tests/resilience-phase12.js
FAIL=true RETRIES=2 VUS=30 k6 run load-tests/resilience-phase12.js
FAIL=false DELAY_MS=1000 VUS=50 k6 run load-tests/resilience-phase12.js
```

For Kafka concurrency, set `KAFKA_CONSUMER_CONCURRENCY=1`, `3`, then `6` when starting the service. Compare lag and DB pool metrics; the supplied script is an HTTP resilience workload, not a substitute for a Kafka producer/lag workload.

## Phase 14 — Full-system load testing

Run services and observability first. Then:

```bash
k6 run load-tests/phase14-smoke.js
MODE=INVENTORY TARGET_VUS=25 STEADY=2m k6 run load-tests/full-system-phase14.js
MODE=MIXED TARGET_VUS=25 STEADY=3m k6 run load-tests/full-system-phase14.js
MODE=PAYMENT TARGET_VUS=10 STEADY=2m k6 run load-tests/full-system-phase14.js
```

`load-tests/phase14-matrix.sh` runs a repeatable initial matrix and writes JSON summaries under `results/phase14/`. The matrix is a starting point; execute profiles separately if they interfere with one another. Prometheus Phase 14 alert rules are mounted from `observability/prometheus/phase14-alert-rules.yml`. Tune alert expressions to the exact metric names emitted by the service version before treating alerts as authoritative.
