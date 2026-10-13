# ADR-0006 — Kafka Eventing Baseline

## Decision

Use Kafka as the asynchronous event transport for payment domain events. Use `paymentId` as the message key and PostgreSQL `processed_payment_events` as the consumer idempotency store.

## Context

The lab needs to demonstrate duplicate delivery, consumer retry, ordering, partitioning, and consumer idempotency before introducing Outbox.

## Alternatives

- Direct synchronous REST calls: simpler, but couples availability and latency between services.
- Kafka without consumer idempotency: asynchronous, but duplicate delivery can repeat business effects.
- Kafka transactions across Kafka and PostgreSQL: not used as the baseline because it would hide the important DB/Kafka atomicity problem that Outbox addresses in Phase 7.

## Consequences

Positive:
- asynchronous decoupling
- partition-based scalability
- replay/redelivery semantics
- explicit consumer group and offset behavior

Negative:
- eventual consistency
- operational complexity
- duplicate delivery remains possible
- ordering is per partition, not global
- DB + Kafka still requires an atomicity pattern for reliable event publication
