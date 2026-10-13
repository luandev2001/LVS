# Phase 4 — Payment Service / External Gateway Simulation

## Goal

Create a realistic payment boundary where the local service cannot assume that an external gateway call is synchronous, reliable, unique, or authoritative without validation.

The implementation intentionally supports failure injection before introducing Kafka/Saga.

## State machine

```text
CREATED -> PENDING -> SUCCEEDED
                  \-> FAILED
                  \-> TIMED_OUT -> SUCCEEDED (late valid callback)

SUCCEEDED -> REFUNDED

FAILED/TIMED_OUT -> PENDING (retry)
```

`TIMED_OUT -> SUCCEEDED` is intentional: a timeout means the local service stopped waiting, not proof that the gateway never completed the transaction. A later valid callback can reconcile the state.

## Service boundary

```text
Client
  |
  | POST /api/payments + Idempotency-Key
  v
Payment Service
  |-- PostgreSQL: payment state
  |
  | simulated outbound gateway transaction
  v
Simulated Gateway
  |
  | asynchronous webhook / IPN
  v
POST /api/payments/{id}/webhook
```

## Transaction boundaries

1. Create payment + move to `PENDING` are one local DB transaction.
2. Gateway callback is a separate transaction.
3. Timeout scanning is a separate transaction per scheduled invocation.
4. Retry creates a new gateway attempt but keeps the same payment aggregate.
5. Refund is a separate state transition.

There is deliberately no distributed transaction between the payment database and gateway.

## Failure scenarios

| Mode | Gateway behavior | Expected local state |
|---|---|---|
| SUCCESS | valid success callback | SUCCEEDED |
| FAILURE | valid failure callback | FAILED |
| TIMEOUT | no callback | TIMED_OUT |
| DELAYED_SUCCESS | callback after timeout window | TIMED_OUT then SUCCEEDED |
| DUPLICATE_SUCCESS | same callback twice | first APPLIED, second DUPLICATE |
| INVALID_SIGNATURE | malformed signature | state unchanged |

## Security checks

The webhook verifies:

- HMAC-SHA256 signature
- gateway reference matches the current attempt
- callback result is applied only from an allowed state
- amount/currency are local payment attributes and are not accepted from the callback as trusted input

The simulated callback signs `paymentId:gatewayReference:success`. The secret is configuration-only and must not be committed as a production secret.

## Important production lesson

A gateway timeout is not equivalent to payment failure. The system needs a reconciliation mechanism (callback, status query, settlement file, or manual reconciliation) before treating an uncertain payment as permanently failed.

## Measurement plan

Do not invent benchmark numbers. Record actual results for:

- payment creation latency
- callback processing latency
- timeout-to-final-state delay
- retry count
- duplicate callback count
- invalid callback rejection count
- database connection usage
- state-transition conflict/error rate

## Known deliberate simplification

The simulated gateway is in-process. It exists to reproduce asynchronous and failure behavior deterministically; it is not a network benchmark and must not be presented as evidence of a real provider's latency/reliability.
