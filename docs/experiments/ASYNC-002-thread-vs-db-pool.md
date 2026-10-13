# ASYNC-002 — Thread Pool versus DB Connection Pool

## Goal

Show that application concurrency and database concurrency are separate limits.

## Matrix

Run the same workload while varying:

| Case | Async max | DB max |
|---|---:|---:|
| A | 4 | 10 |
| B | 8 | 10 |
| C | 16 | 10 |
| D | 32 | 10 |
| E | 16 | 4 |
| F | 16 | 16 |

## Measurements

- p95/p99
- throughput
- `inventory.async.pool.active`
- `inventory.async.pool.queue`
- `inventory.async.rejected`
- Hikari active/pending
- PostgreSQL active sessions

## Expected interpretation

The experiment should identify whether the bottleneck is the application executor, connection pool, or PostgreSQL. Do not infer the winning configuration before measuring.

## Result

[TO BE MEASURED]
