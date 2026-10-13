# ADR-0003 — Compare Four Concurrency Strategies

## Decision
Implement no-lock, optimistic, pessimistic, and atomic SQL reservation paths.

## Reason
The lab must demonstrate how correctness and contention change when the consistency mechanism moves between ORM state management, database row locks, and atomic conditional updates.

## Consequence
The four paths are experiment fixtures. Production selection must be based on measured workload and correctness requirements.
