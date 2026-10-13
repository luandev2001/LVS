# CACHE-003 — Cache Stampede

## Goal

Compare a hot-key expiry with no stampede protection against Redis single-flight protection.

## Setup

Use a short TTL and a controlled database delay, for example:

```text
INVENTORY_CACHE_TTL_SECONDS=3
INVENTORY_CACHE_SIMULATED_DB_DELAY_MS=1000
```

Do not interpret the artificial delay as a production database measurement.

## Naive run

1. Warm a single warehouse/page key.
2. Expire/invalidate the key.
3. Start many concurrent k6 requests with `protection=NAIVE`.
4. Observe the number of PostgreSQL rebuilds.

## Locked run

1. Repeat with `protection=LOCKED`.
2. One caller should acquire the Redis lock.
3. Other callers should wait for the value.
4. Observe lock contention, wait success and PostgreSQL query count.

## Failure edge cases

- loader crashes: lock TTL eventually releases it
- Redis unavailable: API falls back to PostgreSQL
- waiter timeout: request falls back to PostgreSQL and metric records the timeout

## Result

[TO BE MEASURED]
