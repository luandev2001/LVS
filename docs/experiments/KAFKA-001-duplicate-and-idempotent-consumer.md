# KAFKA-001 — Duplicate Message and Idempotent Consumer

## Hypothesis

A Kafka record may be delivered more than once. A database uniqueness constraint on the event ID can make the business-side processing idempotent.

## Setup

1. Start PostgreSQL and Kafka.
2. Start payment-service.
3. Create a payment.
4. Call `POST /api/payments/{id}/events/duplicate`.
5. Inspect application logs and `processed_payment_events`.

## Expected observation

Two Kafka records carry the same `eventId`. The first inserts the event ID. The second finds the event already processed and does not create a second business effect.

## Evidence to capture

- Kafka offsets for both deliveries
- event ID
- partition
- DB row count
- consumer processing timestamps

## Results

[TO BE MEASURED]
