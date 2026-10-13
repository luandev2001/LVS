# Development Roadmap

Phase 0 — Foundation
- repository
- service boundaries
- independent databases
- health/metrics bootstrap

Phase 1 — Inventory baseline
- deliberately naive CRUD
- inefficient queries
- N+1
- missing indexes
- OFFSET pagination

Phase 2 — Database optimization
- EXPLAIN ANALYZE
- indexes
- projections
- keyset pagination
- transaction/connection-pool experiments

Phase 3 — Concurrency
- lost update
- overselling
- optimistic/pessimistic locking
- atomic SQL

Phase 4 — Payment gateway simulator

Phase 5 — Idempotency
- payment idempotency key
- request fingerprint
- concurrent duplicate request race
- duplicate webhook handling

Phase 6 — Kafka

Phase 7 — Outbox
- transactional payment outbox
- SKIP LOCKED batch claiming
- retry/recovery
- duplicate publication window
- polling/batch/concurrency experiments

Phase 8 — Accounting

Phase 9 — Saga

Phase 10 — Redis

Phase 11 — Async processing

Phase 12 — Event Sourcing

Phase 13 — Full observability

Phase 14 — Full-system load and failure experiments

## Phase 10 — Redis cache and failure modes
- Redis cache-aside for inventory reads
- TTL
- after-commit invalidation
- stale-data experiment
- cache stampede reproduction and Redis single-flight mitigation
- Redis failure/fail-open experiment
- load tests and cache/DB/connection-pool metrics

## Phase 11 — Async and backpressure
- CompletableFuture fan-out/fan-in
- bounded executor and explicit rejection/backpressure
- thread-pool versus Hikari/PostgreSQL connection-pool experiments
- async versus sequential load tests
