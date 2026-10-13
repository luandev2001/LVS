# CONC-001 — Overselling / Lost Update

## Goal
Reproduce concurrent stock reservation against one inventory row.

Starting state should be intentionally small, e.g. available stock = 1.

## Strategies
1. `none` — read, modify, save; intentionally unsafe baseline
2. `optimistic` — JPA `@Version`
3. `pessimistic` — `SELECT ... FOR UPDATE` via `PESSIMISTIC_WRITE`
4. `atomic` — conditional SQL update: `quantity - reserved_quantity >= amount`

## Questions
- Can two concurrent requests both observe the same available stock?
- Which strategy serializes access?
- Which strategy rejects conflicts instead of waiting?
- Which strategy minimizes database round trips?
- What happens to latency under contention?

## Evidence
- successful reservations
- rejected reservations
- final reserved quantity
- conflict count
- DB lock/wait behavior
- p95/p99 latency

All measurements: `[TO BE MEASURED]`.
