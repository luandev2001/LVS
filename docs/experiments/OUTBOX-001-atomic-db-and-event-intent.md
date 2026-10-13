# OUTBOX-001 — Atomic Database Change + Event Intent

## Objective

Prove that a payment state change and its event intent are committed in one PostgreSQL transaction.

## Baseline

Phase 6 direct publication:

```text
DB transaction -> commit
Kafka publish
```

Failure window:

```text
DB commit succeeds
Kafka fails
=> payment exists without the corresponding event
```

## Phase 7

```text
DB transaction
  ├── payment mutation
  └── payment_outbox insert
commit

publisher -> Kafka
```

## Procedure

1. Start PostgreSQL and Kafka.
2. Start `payment-service`.
3. Create a payment.
4. Query `/api/outbox/stats`.
5. Stop Kafka.
6. Create another payment.
7. Verify the payment transaction still commits.
8. Verify its outbox row remains pending/failed until Kafka is restored.
9. Start Kafka.
10. Observe the outbox row move to `PUBLISHED`.
11. Verify the event is consumed.

## Evidence to capture

- payment row
- outbox row
- outbox status transitions
- event ID
- Kafka record
- consumer processed-event row
- publication latency

## Expected result

The payment database transaction does not depend on immediate Kafka availability.

## Measurements

- DB transaction latency: `[TO BE MEASURED]`
- outbox insertion latency: `[TO BE MEASURED]`
- pending duration while Kafka is unavailable: `[TO BE MEASURED]`
- recovery/publication latency: `[TO BE MEASURED]`
