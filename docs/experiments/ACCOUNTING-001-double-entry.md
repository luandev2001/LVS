# ACCOUNTING-001 — Double-Entry Journal

## Hypothesis

Every financial event that becomes a journal must satisfy:

`SUM(DEBIT) = SUM(CREDIT)`

## Procedure

1. Create a successful payment.
2. Wait for the Payment Outbox publisher.
3. Wait for Accounting consumption.
4. Query `/api/accounting/journal-entries`.
5. Verify the settlement has one debit and one credit with equal amounts.
6. Refund the payment.
7. Verify the refund creates a separate compensating journal.

## Expected evidence

- journal entry ID
- source event ID
- payment ID
- debit lines
- credit lines
- equal totals
- separate refund journal

No benchmark numbers are claimed until execution.
