# ADR-0007 — Transactional Outbox

## Status
Accepted

## Context

Phase 6 published payment events directly to Kafka after payment state changes. PostgreSQL and Kafka are separate systems, so one local transaction cannot atomically commit both resources.

A failure between the database commit and Kafka publication can lose an event even though the payment transaction succeeded.

The lab also needs to demonstrate the opposite failure window: Kafka publication succeeds but the publisher fails before recording that success.

## Decision

Use a PostgreSQL transactional outbox in `payment-service`.

Whenever a payment business transaction creates or changes a state that must be emitted:

```text
payment mutation
+
outbox insert
```

are committed in the same PostgreSQL transaction.

A separate publisher:

1. claims pending/failed rows using `FOR UPDATE SKIP LOCKED`;
2. changes them to `PROCESSING`;
3. commits the claim transaction;
4. publishes to Kafka outside the database transaction;
5. marks the row `PUBLISHED` after Kafka acknowledges the send;
6. marks failures as retryable `FAILED` rows;
7. requeues stale `PROCESSING` rows.

Consumer-side idempotency remains mandatory.

## Alternatives considered

### Direct Kafka publish

Pros:

- simple
- low implementation overhead
- no outbox table

Cons:

- database/Kafka dual-write failure window
- Kafka outage directly affects event delivery
- recovery requires another mechanism

### Polling Outbox

Pros:

- business state and event intent are atomically persisted
- simple PostgreSQL-based recovery model
- failure and retry behavior is visible and measurable
- `SKIP LOCKED` supports concurrent publishers

Cons:

- polling load
- publication latency depends partly on polling interval
- duplicate publication remains possible
- outbox retention/cleanup is required in a long-lived system

### CDC-based Outbox

Pros:

- database changes can be streamed without application polling
- lower application-level polling overhead

Cons:

- additional CDC infrastructure
- operational complexity
- schema/connector management
- still requires downstream idempotency

## Consequences

Positive:

- payment state and event intent cannot diverge because one commits without the other
- Kafka downtime no longer requires the payment transaction itself to publish synchronously
- retry is durable
- publisher concurrency can be increased independently

Negative:

- an additional table and publisher component are required
- events are eventually, not synchronously, delivered
- duplicate publication is still possible
- outbox rows require monitoring and lifecycle management

## Explicit non-goal

This ADR does not claim exactly-once business processing across PostgreSQL and Kafka.

The design intentionally demonstrates:

```text
transactional outbox + at-least-once publication + idempotent consumer
```
