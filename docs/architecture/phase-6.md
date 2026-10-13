# Phase 6 — Kafka

## Goal

Introduce Kafka after Payment + Idempotency so the lab can demonstrate asynchronous event delivery and the difference between Kafka producer idempotence and application-level consumer idempotency.

## Architecture

```text
Payment Service
    |
    | payment.events.v1
    | key = paymentId
    v
+-------------------+
| Kafka (3 partitions)|
+---------+---------+
          |
          v
Payment Event Consumer
          |
          +--> processed_payment_events (unique event_id)
```

The producer uses `acks=all`, retries, and Kafka producer idempotence. The consumer uses manual acknowledgement and a database unique key for event idempotency.

## Why the paymentId is the Kafka key

All events for one payment are routed to the same partition, preserving partition-local ordering for that key. Ordering is not global across the topic.

## Delivery model

The consumer deliberately uses a non-transactional Kafka + PostgreSQL boundary:

```text
consume
  -> DB commit
  -> acknowledge offset
```

If DB commit succeeds but offset acknowledgement fails, Kafka can redeliver the message. `processed_payment_events.event_id` makes the redelivery harmless.

This is intentionally implemented before Outbox so Phase 7 can demonstrate the separate producer-side DB/Kafka failure window.

## Failure injections

1. Duplicate message: same event ID published twice.
2. Consumer processing exception: `POISON_MESSAGE` triggers retry.
3. Retry exhaustion: Spring Kafka retry flow sends the failed record to a DLT.
4. Consumer restart: offsets are not auto-committed.
5. Slow consumer: can be reproduced by adding processing delay and observing consumer lag.

## Producer idempotence vs consumer idempotence

Kafka producer idempotence protects Kafka from certain duplicate writes caused by producer retries. It does **not** mean that an application cannot publish the same business event twice with different producer operations.

Consumer idempotency is therefore still required for business effects.

## Partitioning

The topic has three partitions. `paymentId` is the key. The experiment should verify that events for one payment remain ordered while different payments can be processed in parallel.

## Measurements

All measurements are intentionally left as `[TO BE MEASURED]`:

- producer throughput
- consumer throughput
- end-to-end event latency
- consumer lag
- partition distribution
- retry count
- DLT count
- duplicate deliveries
- DB processing latency

## Phase 7 boundary

Direct DB business transaction + Kafka publish still has a failure window. Phase 7 will introduce the Outbox Pattern rather than pretending Kafka producer retries solve DB/Kafka atomicity.
