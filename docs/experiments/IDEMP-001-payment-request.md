# IDEMP-001 — Payment Request Idempotency

## Goal

Demonstrate that client retries using the same `Idempotency-Key` do not create a second payment business object, including when duplicate requests race concurrently.

## Baseline problem

A naive implementation can do:

```text
SELECT payment WHERE idempotency_key = K
        ↓
not found
        ↓
INSERT payment
```

Two concurrent requests can both observe `not found` before either insert commits.

Application-level `if exists` is therefore not the final concurrency guarantee.

## Phase 5 implementation

The service uses three layers:

1. **Request fingerprint** — binds an idempotency key to the original business parameters.
2. **PostgreSQL UNIQUE constraint** — `uk_payment_idempotency_key` is the final race arbiter.
3. **Race recovery** — if the insert loses a unique-key race, the transaction is rolled back and the committed winner is loaded.

A reused key with different parameters is rejected rather than silently returning a different payment.

## Expected invariants

For one idempotency key K:

```text
count(payments where idempotency_key = K) = 1
```

For repeated identical requests:

```text
same key + same parameters → same payment ID
```

For conflicting reuse:

```text
same key + different parameters → HTTP 409
```

## Measurement

Record:

- number of concurrent requests
- successful responses
- conflict responses
- duplicate payment rows
- database unique-constraint violations
- p50/p95/p99 latency
- DB connection usage

All numeric results:

`[TO BE MEASURED]`
