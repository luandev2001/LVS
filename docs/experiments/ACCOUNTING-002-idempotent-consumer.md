# ACCOUNTING-002 — Idempotent Accounting Consumer

## Hypothesis

Redelivery of the same Kafka event must not create a second accounting effect.

## Procedure

1. Create a successful payment.
2. Record the resulting `journal_entry` count and `source_event_id`.
3. Inject duplicate publication using the Phase 7 duplicate-event endpoint.
4. Observe Kafka delivery and Accounting metrics/logs.
5. Query Accounting again.

## Expected behavior

The same `eventId` is already present in `processed_payment_events`, so Accounting acknowledges the duplicate without creating another journal.

If a concurrent delivery races before the idempotency row is visible, PostgreSQL uniqueness remains the final protection.

No benchmark numbers are claimed until execution.
