# IDEMP-002 — Duplicate Webhook Delivery

## Goal

Demonstrate that repeated delivery of the same valid gateway callback does not apply the payment transition twice.

## Failure scenario

```text
Gateway
  ├── callback #1 ──→ Payment Service
  └── callback #2 ──→ Payment Service
```

The two callbacks may arrive close together because of gateway retries, client/network uncertainty, or duplicate delivery.

## Protection

Webhook processing acquires a database row lock for the payment before validating/applying the state transition.

The first transaction changes:

```text
PENDING → SUCCEEDED
```

The second transaction waits for the row lock, reloads the committed state, and observes that the payment is already `SUCCEEDED`.

The domain state machine returns `DUPLICATE` instead of applying another business effect.

## Important distinction

This is **idempotent state transition handling**. It is not yet a Kafka idempotent consumer; Kafka experiments belong to the later messaging phases.

## Measurement

Record:

- duplicate callback count
- applied callback count
- duplicate callback outcome
- payment final status
- callback latency
- lock wait time where observable

All numeric results:

`[TO BE MEASURED]`
