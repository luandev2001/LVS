# ADR-0008 — Saga Choreography

## Decision
Use choreography for Payment → Accounting → Inventory.

## Local reliability rule
Payment, Accounting and Inventory emit cross-service events through local transactional outboxes. A consumer transaction updates local state and inserts its next event into the same local database transaction.

## Alternatives
- Orchestration: central workflow state and explicit compensation commands; easier global visibility but introduces a coordinator boundary.
- 2PC/distributed ACID: rejected for this independent-database + Kafka architecture.

## Consequences
Positive: autonomy, eventual consistency, failure isolation, reusable outbox pattern. Negative: distributed event contracts, duplicate handling, timeouts, compensation logic and more difficult global debugging.
