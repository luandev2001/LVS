# KAFKA-004 — Consumer concurrency and backpressure

## Hypothesis
Throughput improves with concurrency only until partitions, CPU, DB connections, or downstream capacity saturate. Beyond that, latency/lag/errors can worsen.

## Matrix
- `KAFKA_CONSUMER_CONCURRENCY=1,3,6`
- `KAFKA_MAX_POLL_RECORDS=10,20,50`
- Keep topic partition count fixed at 3 for the first pass, then repeat with more partitions.

## Record
Messages/sec, consumer lag, processing p95/p99, DB active/pending connections, CPU, retries, duplicate processing, rebalance count. `[TO BE MEASURED]`.

## Interpretation
If concurrency > partition count, extra threads do not provide additional partition parallelism. If DB wait grows while lag does not improve, the DB is the bottleneck. If lag grows with low DB utilization, inspect handler time, poll interval, and downstream calls.
