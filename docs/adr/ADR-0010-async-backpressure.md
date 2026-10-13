# ADR-0010 — Bounded Async Executor and Explicit Backpressure

## Status
Accepted for Phase 11 simulation.

## Context

`CompletableFuture` can release the servlet thread while independent blocking DB operations execute concurrently. However, the application still has finite executor threads, DB connections and database capacity.

An unbounded executor can convert traffic spikes into unbounded queue growth. `CallerRunsPolicy` can hide executor saturation by making the request thread perform the overflow task.

## Decision

Use a bounded `ThreadPoolTaskExecutor` for inventory fan-out with:

- configurable core/max pool size
- bounded queue
- `AbortPolicy`
- HTTP 429 on rejection
- separate Spring transactional DB-reader per page task
- metrics for active workers, queue depth and rejection

## Consequences

Positive:

- concurrency is explicit and measurable
- overload is visible
- servlet threads are not used as accidental worker threads
- executor and DB-pool experiments can be isolated

Trade-offs:

- callers can receive 429 under load
- queue sizing becomes an operational tuning parameter
- blocking JDBC still consumes a DB connection per active transaction

## Rejected alternatives

### Unbounded queue
Can hide overload until latency/memory becomes unacceptable.

### CallerRunsPolicy
Can turn executor saturation into servlet-thread saturation.

### Reactive stack
Would require an end-to-end non-blocking model and reactive database access; it is outside this phase's goal of understanding blocking JDBC plus explicit Java concurrency.
