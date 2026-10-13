# ADR-0004 — Explicit Payment State Machine

## Status
Accepted

## Decision

Represent payment lifecycle as an explicit enum-backed state machine and reject invalid transitions in the domain model.

## Context

External payment providers are asynchronous and can deliver duplicate, delayed, or invalid callbacks. A boolean `paid=true/false` model cannot represent uncertainty, retries, or refund lifecycle safely.

## Consequences

Positive:

- lifecycle is explicit
- invalid transitions are easier to detect
- timeout and late callback can be modeled
- retries have visible attempt numbers

Trade-offs:

- more states and transition rules
- state-machine rules need tests
- eventual consistency remains across the external gateway boundary

## Alternatives considered

### Boolean paid flag
Too little information for timeout, pending, retry, and refund semantics.

### Distributed transaction
Not appropriate for a generic external gateway boundary because the external provider does not participate in the local database transaction.
