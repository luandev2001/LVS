# ADR-0008 — Accounting Consumer and Phase 8 Observability

## Context

Phase 7 proved transactional outbox publication. The system now needs a real downstream business effect and enough telemetry to diagnose asynchronous propagation.

## Decision

1. Keep Accounting as a separate service and PostgreSQL database.
2. Consume `payment.events.v1` using a dedicated consumer group.
3. Represent financial effects as double-entry journal entries.
4. Enforce debit/credit equality in application logic and protect event uniqueness in PostgreSQL.
5. Use `processed_payment_events.event_id` as the event-consumer idempotency key.
6. Use Micrometer/Actuator + Prometheus for metrics.
7. Use Micrometer Tracing + OpenTelemetry for trace export.
8. Use ECS structured console logs.

## Why not update Payment and Accounting in one transaction?

They own different databases and services. A local database transaction cannot atomically commit both systems. A distributed transaction coordinator would add coupling and operational complexity that is outside the current simulation objective.

## Why reversal instead of destructive update?

Financial history should remain auditable. A refund is modeled as a compensating journal rather than modifying the original settlement journal.

## Consequences

Positive:

- realistic eventual-consistency workflow
- duplicate-safe event consumption
- auditable accounting history
- metrics and traces for diagnosis

Trade-offs:

- accounting is temporarily stale after a payment change
- duplicate events still reach the consumer
- observability introduces configuration and runtime overhead
- consumer lag must be monitored explicitly
