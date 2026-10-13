# DB-004 — OFFSET vs Keyset Pagination

## Baseline
`GET /api/inventory?warehouseCode=WH-1&page=N&size=50`

Spring Data `Page` uses offset pagination and a count query.

## Optimized variant
`GET /api/inventory/keyset?warehouseCode=WH-1&cursor=<last-id>&size=50`

The query uses the indexed `(warehouse_code, id DESC)` path and a cursor predicate.

## Important control
Use the same dataset, page size, warehouse, test duration and load profile. Compare shallow and deep pages separately.

## Evidence
- query latency
- count-query cost
- rows scanned
- execution plan
- throughput

Metrics: `[TO BE MEASURED]`.
