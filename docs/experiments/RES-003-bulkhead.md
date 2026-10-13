# RES-003 — Bulkhead isolation

Set `RESILIENCE_BULKHEAD_PERMITS=4`, then run concurrent calls to `/api/resilience-lab/dependency?delayMs=1000`. Calls beyond the semaphore capacity should be rejected with 429 rather than accumulating unlimited in-flight dependency work. Compare with permits 8 and 16. Record rejection rate and p95; `[TO BE MEASURED]`.
