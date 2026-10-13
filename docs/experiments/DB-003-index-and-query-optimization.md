# DB-003 — Index and Query Optimization

## Goal
Compare the Phase 1 baseline with an indexed implementation without changing the workload.

## Baseline
- `GET /api/inventory`
- no `warehouse_code` index in the original schema
- lazy product access remains the N+1 baseline

## Optimized variants
- `GET /api/inventory/optimized` uses `JOIN FETCH`
- composite index `(warehouse_code, id DESC)` supports filtering and ordered pagination
- `product_id` index supports product-based access paths

## Evidence to collect
- p50/p95/p99 latency
- requests/sec
- PostgreSQL EXPLAIN ANALYZE
- rows scanned / returned
- DB CPU
- DB connections
- SQL statement count

All values remain `[TO BE MEASURED]` until the benchmark is executed.
