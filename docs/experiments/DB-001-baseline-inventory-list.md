# DB-001 — Baseline Inventory List

## Objective
Establish a deliberately non-optimized baseline for the inventory list API.

## Endpoint
`GET /api/inventory?warehouseCode=WH-1&page=0&size=50`

## Intentional problems
1. `inventory.warehouse_code` has no index.
2. `Inventory.product` is LAZY, but the mapper accesses product fields for every row, producing an N+1 query pattern.
3. Pagination uses Spring Data `Page`, therefore the baseline uses OFFSET/LIMIT plus a count query.
4. The query/entity mapping is intentionally not optimized with projections or fetch joins.

## Measurement
Record actual results only:

- p50 latency: [TO BE MEASURED]
- p95 latency: [TO BE MEASURED]
- p99 latency: [TO BE MEASURED]
- throughput: [TO BE MEASURED]
- PostgreSQL CPU: [TO BE MEASURED]
- DB connections: [TO BE MEASURED]
- rows scanned: [TO BE MEASURED]
- rows returned: [TO BE MEASURED]
- SQL query count per request: [TO BE MEASURED]

## Evidence
- k6 result
- PostgreSQL `EXPLAIN (ANALYZE, BUFFERS)`
- application metrics
- SQL/query-count evidence

## Next optimization
Do not optimize until this baseline has been measured. The first candidate is query/index optimization, followed by removal of N+1 and pagination experiments.
