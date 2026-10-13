# PAY-002 — Timeout, Retry, and Delayed Callback

## Purpose

Demonstrate that an external payment call can become uncertain: the caller can time out while the provider later completes the transaction.

## Scenarios

### A. Timeout

Create with `mode=TIMEOUT`.

Expected: payment reaches `TIMED_OUT` after the configured timeout scanner window.

### B. Delayed success

Create with `mode=DELAYED_SUCCESS`.

The simulated callback is deliberately later than the local timeout.

Expected: `PENDING -> TIMED_OUT -> SUCCEEDED` after a valid late callback.

### C. Retry

Create with `mode=TIMEOUT`, wait for timeout, then retry with `mode=SUCCESS`.

Expected: attempt increments and the new gateway reference becomes the active reference.

## Measurement

Record:

- timeout detection delay
- callback delay
- retry latency
- finalization latency
- duplicate/late callback outcomes

Results: [TO BE MEASURED]
