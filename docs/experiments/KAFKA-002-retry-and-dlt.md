# KAFKA-002 — Retry and Dead Letter Topic

## Hypothesis

A transient consumer failure should cause redelivery/retry rather than silently losing the record. A permanently failing record should eventually be routed to a DLT.

## Failure injection

Publish a `PaymentEvent` whose `eventType` is `POISON_MESSAGE`.

## Expected behavior

The consumer throws intentionally. Spring Kafka Retryable Topic infrastructure retries the record. After the configured attempts, the record is routed to the DLT.

## Measurements

- retry attempts: [TO BE MEASURED]
- retry delay: [TO BE MEASURED]
- DLT records: [TO BE MEASURED]
- consumer lag: [TO BE MEASURED]
