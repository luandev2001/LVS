# ASYNC-003 — Async Executor Saturation and Backpressure

## Goal

Demonstrate why bounded concurrency needs an overload policy.

## Scenario

Configure a small executor, for example:

```text
core = 2
max = 2
queue = 2
```

Then run:

```bash
VUS=100 PAGES=8 k6 run load-tests/async-backpressure-phase11.js
```

## Expected behavior

Once workers and queue are full, new fan-out work is rejected and the API returns HTTP 429 rather than growing an unbounded queue or executing overflow work on the servlet thread.

## Measurements

- 429 rate
- executor queue depth
- active workers
- request latency
- JVM memory
- Hikari pending

## Result

[TO BE MEASURED]
