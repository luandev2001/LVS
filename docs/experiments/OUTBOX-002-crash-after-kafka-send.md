# OUTBOX-002 — Crash Window After Kafka Send

## Objective

Demonstrate that Outbox solves the database/event-intent dual write, but does not make Kafka publication exactly-once.

## Injection

Run with:

```bash
OUTBOX_SIMULATE_CRASH_AFTER_SEND=true
```

The publisher intentionally throws after `KafkaTemplate.send(...).get()` succeeds and before the outbox row is marked `PUBLISHED`.

## Procedure

1. Start PostgreSQL and Kafka.
2. Start the service with crash simulation enabled.
3. Create a payment.
4. Observe the outbox row become `PROCESSING`.
5. Confirm Kafka receives the event.
6. Observe that the row is not marked `PUBLISHED`.
7. Wait for stale-processing recovery or call:

```bash
curl -X POST http://localhost:8084/api/outbox/recover-stale
```

8. Disable crash simulation.
9. Allow the publisher to retry.
10. Observe the same `eventId` reach Kafka again.
11. Verify the consumer detects the duplicate using `processed_payment_events.event_id`.

## Expected result

```text
Kafka receives event #1
publisher status update is lost
outbox is retried
Kafka receives event #2 with the same eventId
consumer processes the business effect once
```

## Important distinction

This is a deliberate simulation of a crash window. It does not prove that Kafka or PostgreSQL is exactly-once.

## Measurements

- time from first send to duplicate send: `[TO BE MEASURED]`
- duplicate deliveries: `[TO BE MEASURED]`
- consumer duplicate detections: `[TO BE MEASURED]`
- business effects applied twice: `[TO BE MEASURED]`
