# Phase 12 — Kafka Concurrency, Backpressure, and Resilience

## Scope
This phase combines (A) Kafka consumer parallelism/backpressure and (B) dependency resilience with bounded retry, circuit breaker, and bulkhead experiments.

## Kafka consumer concurrency
Listeners use `${lab.kafka.consumer-concurrency:3}`; tune with `KAFKA_CONSUMER_CONCURRENCY`. Start at 1, then 3, then 6. A consumer group cannot actively consume more partitions than the topic has; the local Kafka topic defaults to three partitions. Extra listener threads can therefore sit idle. Tune `KAFKA_MAX_POLL_RECORDS` and `KAFKA_MAX_POLL_INTERVAL_MS` with actual processing latency in mind. Increasing concurrency can increase DB contention and Hikari wait rather than throughput.

## Resilience lab endpoint
`GET /api/resilience-lab/dependency?fail=true&delayMs=100&retries=2` deliberately simulates a failing/slow dependency. Success returns attempts and elapsed time. Circuit-open returns 503; bulkhead exhaustion returns 429; exhausted downstream failures return 502.

The small in-house implementation exists to make mechanics visible in a learning lab. It is not production-grade and should be compared against Resilience4j before real deployment. The endpoint has a semaphore bulkhead, bounded retries, and a CLOSED/OPEN/HALF_OPEN breaker with configurable failure threshold and open duration.

## Experiments
1. Kafka consumer concurrency: compare 1/3/6 and record throughput, lag, processing p95, DB pool active/pending, duplicate count and rebalance events.
2. Slow dependency without delay: `delayMs=0`; then with `delayMs=300`.
3. Retry amplification: `fail=true&retries=0` vs `retries=2`; count downstream attempts, not only HTTP requests.
4. Circuit breaker: repeatedly call `fail=true`; verify later requests are rejected quickly with 503, wait `RESILIENCE_CIRCUIT_OPEN_MS`, then send a successful probe.
5. Bulkhead: run high concurrency with `delayMs=1000`; observe 429s while bounded in-flight calls remain near `RESILIENCE_BULKHEAD_PERMITS`.
6. Recovery: restore the dependency (`fail=false`) after open interval and observe transition to CLOSED.

## Important semantics
- Kafka listener concurrency is bounded by partition count and consumer-group assignment.
- At-least-once delivery means handlers must remain idempotent; retries and rebalances can redeliver.
- Retry only transient failures. Do not retry deterministic validation/business errors indefinitely.
- Retries multiply load during an outage. Use a small retry budget, backoff, and a circuit breaker.
- Bulkhead isolates concurrent calls but does not make the dependency faster.
- Circuit breaker does not cancel already-running work and is not a substitute for timeouts.
- A Kafka consumer that blocks too long may exceed `max.poll.interval.ms` and trigger reassignment; keep processing duration and polling configuration aligned.

## Measurement policy
No benchmark values are pre-filled. Capture environment, partition count, payload size, consumer concurrency, retry policy, DB pool size, request rate, error rate, consumer lag, p50/p95/p99, and CPU/memory. Results remain `[TO BE MEASURED]` until the scripts are run.
