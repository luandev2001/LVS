# RES-001 — Retry amplification

Call `/api/resilience-lab/dependency?fail=true&delayMs=100&retries=0`, then repeat with `retries=2` under the same arrival rate. Compare total attempts and response time. Retries can improve transient recovery but multiply traffic during persistent failure. Record attempts/request and p95; `[TO BE MEASURED]`.
