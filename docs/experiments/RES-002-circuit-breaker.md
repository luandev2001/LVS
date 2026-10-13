# RES-002 — Circuit breaker

Use `GET /api/resilience-lab/dependency?fail=true` repeatedly until the configured threshold is reached. Verify calls are rejected quickly with 503 while OPEN. Wait `RESILIENCE_CIRCUIT_OPEN_MS`, then call with `fail=false` to exercise HALF_OPEN recovery. Record open duration, rejected calls, recovery time; `[TO BE MEASURED]`.
