# Phase 7 — Transactional Outbox

## Goal

Close the producer-side database/Kafka dual-write failure window demonstrated in Phase 6.

Phase 6 had:

```text
DB transaction
    |
    +--> COMMIT
    |
    +--> Kafka publish
```

There is no atomic transaction spanning PostgreSQL and Kafka in this design. A crash or Kafka outage can therefore leave the database committed while the event was never published.

Phase 7 changes the producer boundary to:

```text
Business transaction
    |
    +--> business row(s)
    |
    +--> outbox row
    |
    +--> COMMIT

separate publisher
    |
    +--> claim outbox rows
    +--> COMMIT claim
    +--> Kafka publish
    +--> mark PUBLISHED
```

The business state and the fact that an event must be published are now committed atomically in PostgreSQL.

## Architecture

```text
                     PostgreSQL payment_db
                  +--------------------------+
                  | payments                 |
Payment request ->|                          |
                  | payment_outbox           |
                  +------------+-------------+
                               |
                         poll + claim
                         FOR UPDATE
                         SKIP LOCKED
                               |
                               v
                     Outbox Publisher
                     batch + concurrency
                               |
                               v
                         Kafka topic
                      payment.events.v1
                               |
                               v
                    Payment Event Consumer
                               |
                               v
                 processed_payment_events
```

## Outbox lifecycle

```text
PENDING
   |
   | claim transaction
   v
PROCESSING
   |
   +---- Kafka success ----> PUBLISHED
   |
   +---- Kafka failure ----> FAILED --retry--> PROCESSING
   |
   +---- publisher crash --> PROCESSING
                              |
                              | stale recovery
                              v
                           PENDING
```

`PROCESSING` is intentionally not treated as permanent. A stale processing row is requeued after `stale-processing-ms`.

## Why claim before Kafka publish?

Do not hold a PostgreSQL row lock while waiting for Kafka/network I/O.

The publisher first claims rows in a short transaction:

```sql
SELECT ...
FROM payment_outbox
WHERE status IN ('PENDING', 'FAILED')
  AND available_at <= current_timestamp
ORDER BY created_at
LIMIT ?
FOR UPDATE SKIP LOCKED;
```

The rows are changed to `PROCESSING` and the transaction commits before Kafka is contacted.

This gives multiple publisher workers safe ownership without turning Kafka latency into database lock duration.

## Atomicity that Outbox provides

Inside the payment database transaction:

```text
payment state change
+
outbox insert
= one PostgreSQL transaction
```

If the transaction rolls back, neither the business change nor its outbox event exists.

If the transaction commits, the event is durably present in the outbox even when Kafka is unavailable at that moment.

## What Outbox does NOT provide

Outbox does not create an atomic PostgreSQL + Kafka transaction.

There is still a second failure window:

```text
Kafka send succeeds
        |
        X publisher crashes
        |
        X before outbox row becomes PUBLISHED
```

The row can later be retried, causing the same event to be published again.

Therefore:

```text
Outbox
    +
Idempotent consumer
```

is the intended reliability model for this phase.

The consumer's `processed_payment_events.event_id` primary key prevents repeated business processing for the same event ID.

## Duplicate publishing experiment

Set:

```text
OUTBOX_SIMULATE_CRASH_AFTER_SEND=true
```

The publisher sends the Kafka record and deliberately fails before changing the outbox row to `PUBLISHED`.

Expected observable sequence:

```text
PENDING
 -> PROCESSING
 -> Kafka receives event
 -> simulated crash window
 -> row remains PROCESSING
 -> stale recovery
 -> PENDING
 -> Kafka receives same event again
 -> consumer sees duplicate event_id
 -> business effect is not repeated
```

This is a simulation of the critical crash window; it is not a claim that the process literally terminated.

## Polling interval

Configuration:

```yaml
payment:
  outbox:
    polling-ms: 500
```

Smaller polling intervals can reduce publication latency but increase database polling/claim activity.

Larger intervals reduce polling pressure but increase the time an event may wait in `PENDING`.

Measure both instead of assuming the smallest interval is optimal.

## Batch size

Configuration:

```yaml
payment:
  outbox:
    batch-size: 50
```

A larger batch reduces transaction/claim overhead but can increase:

- memory held by a worker
- Kafka burst size
- recovery/retry granularity
- time before a row is marked `PUBLISHED`

The optimal batch size is workload-dependent.

## Publisher concurrency

Configuration:

```yaml
payment:
  outbox:
    publisher-concurrency: 2
```

Workers use `SKIP LOCKED`, so independent workers can claim different rows without waiting on each other.

More concurrency is not automatically better because Kafka, CPU, network, and PostgreSQL can become the new bottleneck.

## Direct publish vs Outbox

| Approach | DB/event atomicity | Kafka outage behavior | Duplicate risk | Complexity |
|---|---|---|---|---|
| Direct publish | No | Business transaction can succeed while publish fails | Yes | Low |
| Polling Outbox | DB change + event intent atomic | Event waits in DB and retries | Yes | Medium |
| CDC-based Outbox | DB change + event intent atomic | CDC pipeline retries downstream | Yes | Higher |

The lab uses polling Outbox because PostgreSQL is already the system of record and the implementation makes polling interval, batch size, locking, retry, and failure windows directly measurable.

CDC remains a comparison topic rather than being introduced before the polling mechanism is understood.

## Consistency model

Producer side:

- Strong atomicity between payment DB state and outbox intent.
- Event publication is asynchronous.

System-wide:

- Eventual consistency between PostgreSQL and Kafka consumers.
- At-least-once event publication is the practical model.
- Consumer idempotency is required.

## Measurements

All values remain `[TO BE MEASURED]` until runtime experiments are executed.

Measure:

- outbox insert latency
- outbox backlog size
- oldest pending event age
- publication latency
- publisher throughput
- Kafka producer throughput
- failed publication count
- retry count
- duplicate publication count
- consumer duplicate count
- DB CPU/connections
- Kafka lag
- batch-size impact
- polling-interval impact
- publisher-concurrency impact

## Phase 7 evidence

The important interview evidence is not a benchmark number by itself. It is the failure sequence:

```text
DB commit succeeds
Kafka unavailable
=> event remains in outbox
=> publisher retries later
=> event is eventually published
```

and:

```text
Kafka publish succeeds
publisher crashes before status update
=> event can be published twice
=> consumer idempotency prevents duplicate business effect
```

## Next boundary

Phase 8 introduces Accounting so payment events have a real downstream business effect rather than only a processed-event record.
