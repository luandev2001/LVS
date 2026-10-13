# CACHE-001 — Cache-Aside Hit/Miss

## Hypothesis

After the first miss populates Redis, repeated reads of the same warehouse page should avoid the PostgreSQL query until TTL expiry or invalidation.

## Procedure

1. Start Redis and inventory DB.
2. Run the optimized endpoint and record baseline latency/query behavior.
3. Call `/api/inventory/cached?warehouseCode=WH-1&page=0&size=50&protection=LOCKED` once.
4. Repeat the same request many times.
5. Compare PostgreSQL query count and latency with the baseline.

## Evidence

- Redis hit/miss counters
- PostgreSQL query/QPS
- Hikari metrics
- k6 latency/throughput

## Result

[TO BE MEASURED]
