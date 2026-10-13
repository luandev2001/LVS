# ADR-0005 — Payment Idempotency Strategy

## Context

Payment creation can be retried after a client timeout even though the original request may already have committed. Gateway callbacks can also be delivered more than once.

## Decision

Use:

1. Idempotency key supplied by the client.
2. Persistent unique constraint on `payments.idempotency_key`.
3. Request fingerprint to prevent semantic reuse of a key.
4. Database-backed race recovery after unique-key contention.
5. Pessimistic row locking during webhook state transition.
6. Domain state validation as the final business guard.

## Consequences

The guarantee survives multiple application instances and process restarts because the source of truth is PostgreSQL rather than JVM memory.

Database constraint exceptions are expected as a possible part of the concurrency path and must be handled deliberately.

Redis distributed locking is intentionally deferred until a later experiment where it solves a demonstrated cross-resource problem.
