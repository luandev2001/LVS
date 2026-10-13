# Phase 5 — Idempotency

## Objective

Protect payment creation and gateway callback processing from duplicate delivery and client retries without relying on application memory or timing assumptions.

## Architecture

```text
Client
  │
  │ POST /api/payments
  │ Idempotency-Key: K
  ▼
Payment Service
  │
  ├─ validate request
  ├─ calculate request fingerprint
  ├─ lookup K
  │
  ├─ existing + same fingerprint ──→ return existing payment
  │
  └─ not found
       │
       ▼
    PostgreSQL
    UNIQUE(idempotency_key)
       │
       ├─ winner → create payment
       └─ loser  → unique violation → rollback → load winner
```

Webhook path:

```text
Gateway callback
      ↓
signature validation
      ↓
payment row FOR UPDATE
      ↓
amount/reference validation
      ↓
state-machine transition
      ↓
commit
```

## Why the unique constraint matters

A pre-insert existence check is not sufficient under concurrency. The database constraint closes the race window.

## Why request fingerprint matters

An idempotency key represents one logical operation. Reusing the same key for different amount/order/currency must not silently return the result of an unrelated request.

## Consistency model

Payment creation is strongly consistent within the payment database. External gateway processing remains asynchronous and can be delayed, duplicated, or lost.

## Trade-offs

### Unique constraint

Pros:

- concurrency-safe at the database boundary
- simple
- durable
- works across multiple application instances

Cons:

- duplicate races may generate database constraint exceptions
- requires recovery logic

### Distributed/application lock

Not used as the primary mechanism here. An in-memory lock would fail when multiple service instances are running.

### Redis lock

Deferred. It is not introduced merely to solve a database uniqueness problem.

## Failure cases

| Scenario | Expected result |
|---|---|
| Same key, same parameters | Same payment returned |
| Same key, concurrent requests | One payment created |
| Same key, different parameters | Conflict |
| Duplicate webhook | No second state transition |
| Delayed webhook after timeout | Valid state reconciliation |
| Invalid webhook signature | No state change |

## Benchmark

No performance numbers are fabricated.

- duplicate request throughput: `[TO BE MEASURED]`
- p95 latency: `[TO BE MEASURED]`
- duplicate payment count: `[TO BE MEASURED]`
- callback duplicate rate: `[TO BE MEASURED]`
