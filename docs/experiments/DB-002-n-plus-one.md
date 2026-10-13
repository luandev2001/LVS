# DB-002 — N+1 Query Experiment

## Hypothesis
The inventory list baseline loads one page of inventory rows, then lazily loads the associated Product when mapping each row. This should create an N+1 query pattern.

## Baseline
`GET /api/inventory?warehouseCode=WH-1&page=0&size=50`

Expected query shape to verify from actual SQL logs:

1. one query for the inventory page
2. one additional product query per distinct product accessed
3. one count query because `Page` is returned

The exact count must be measured, not assumed, because ORM behavior can depend on identity-map reuse and query execution.

## Measurement
Enable Hibernate SQL/bind logging only for this experiment. Record:

- SQL statements per request: [TO BE MEASURED]
- p50: [TO BE MEASURED]
- p95: [TO BE MEASURED]
- p99: [TO BE MEASURED]
- DB CPU: [TO BE MEASURED]

## Optimization candidate
Compare the baseline with a fetch join and/or DTO projection. Do not implement the optimized version until the baseline evidence is captured.
