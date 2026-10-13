# ADR-0009 — Redis Cache-Aside and Versioned Invalidation

## Status
Accepted for Phase 10 simulation.

## Context

The inventory service now has an optimized PostgreSQL read path. Repeated reads of the same warehouse page still consume application and database resources. The lab also needs to demonstrate stale data and cache stampede rather than hiding those failure modes behind a framework annotation.

## Decision

Use Redis with an explicit cache-aside implementation:

- Redis GET before PostgreSQL
- PostgreSQL on miss
- Redis SET with TTL after a successful read
- after-commit invalidation after inventory mutation
- versioned warehouse namespace for list invalidation
- Redis distributed single-flight lock for stampede mitigation
- fail-open to PostgreSQL when Redis is unavailable

## Consequences

### Positive

- shared cache across application instances
- explicit TTL and invalidation semantics
- no key scan required for warehouse list invalidation
- stampede mitigation can be observed and measured
- cache infrastructure is not a hard dependency for correctness

### Negative

- additional network hop and serialization cost on cache hit
- stale data remains possible if invalidation is missed or deliberately disabled
- Redis becomes another operational dependency
- distributed locking adds complexity and waiting behavior
- versioned invalidation leaves unreachable old keys until TTL

## Rejected alternatives

Caffeine remains useful for process-local hot data. It is not the primary experiment because it cannot by itself provide shared state across service instances.

Spring Cache is not rejected as a production option; it is intentionally not the primary implementation here because the lab needs to expose the underlying Redis key, TTL, invalidation and lock mechanics.
