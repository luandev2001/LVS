# Phase 9 — Saga Choreography: Payment → Accounting → Inventory

## Goal
Complete the distributed business flow without a central orchestrator. Each service owns a local transaction and reacts to domain events.

## Flow
```text
Payment SUCCEEDED
  ↓ payment outbox
Kafka PAYMENT_SUCCEEDED
  ↓
Accounting
  ├─ settlement journal
  └─ accounting outbox → ACCOUNTING_POSTED
                         ↓
                      Inventory
                        ├─ atomic reservation → INVENTORY_RESERVED
                        └─ failure → INVENTORY_RESERVATION_FAILED
                                      ↓
                                  Accounting
                                    └─ reversal journal
                                       ↓ ACCOUNTING_REVERSED
                                    Payment
                                      └─ REFUNDED + COMPENSATED
```

## Saga states
`WAITING_ACCOUNTING → WAITING_INVENTORY → COMPLETED`; failure: `WAITING_INVENTORY → COMPENSATING → COMPENSATED`; timeout: `WAITING_* → TIMED_OUT`.

## Failure injection
- `sagaFailureMode=ACCOUNTING_FAIL`: Accounting retries/DLTs; Payment eventually times out.
- `sagaFailureMode=INVENTORY_FAIL`: Inventory emits failure; Accounting posts a reversal; Payment becomes compensated/refunded.
- Duplicate delivery: durable event-id boundaries prevent repeated reservation/journal effects.

## Consistency
This is eventual consistency, not distributed ACID. Every event-producing service uses its own local outbox. Cross-service progress is represented by events and Payment saga state.

## Choreography trade-off
No central coordinator, but the workflow knowledge is distributed across event consumers. As the number of participants grows, event contracts and failure paths become harder to reason about; that is the point at which orchestration becomes an alternative to evaluate.

## Measurements
Record Saga completion latency, state dwell time, compensation count, inventory failures, Kafka lag, each service's outbox backlog, duplicate count, retries/DLT and journal balance. Values: `[TO BE MEASURED]`.

## Example API
`POST /api/payments?orderReference=ORDER-1&amount=100&currency=USD&mode=SUCCESS&inventoryId=1&quantity=1&sagaFailureMode=NONE` with `Idempotency-Key`.

Change `sagaFailureMode` to `INVENTORY_FAIL` or `ACCOUNTING_FAIL` to inject the corresponding failure.
