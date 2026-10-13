# PAY-001 — Payment State Machine

## Hypothesis

Explicit state transitions prevent accidental business transitions such as `FAILED -> SUCCEEDED` without a new attempt or `PENDING -> REFUNDED`.

## Scenarios

1. SUCCESS callback from PENDING -> SUCCEEDED.
2. FAILURE callback from PENDING -> FAILED.
3. TIMEOUT -> TIMED_OUT.
4. DELAYED_SUCCESS -> TIMED_OUT -> SUCCEEDED.
5. SUCCEEDED -> REFUNDED.
6. Duplicate success callback -> no second business effect.
7. Invalid signature -> state unchanged.
8. Reference mismatch -> state unchanged.

## Evidence to capture

- request/response payloads
- payment status before and after
- attempt number
- gateway reference
- callback outcome
- timestamps

## Results

[TO BE MEASURED]
