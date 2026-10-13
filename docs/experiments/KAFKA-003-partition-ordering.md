# KAFKA-003 — Partitioning and Ordering

## Experiment

Publish multiple events using the same `paymentId` key and publish events for several different payment IDs.

## Expected behavior

All records for one payment key are assigned to the same partition. Kafka preserves order within that partition. There is no global ordering guarantee across partitions.

## Measurements

- partition distribution: [TO BE MEASURED]
- per-key ordering violations: [TO BE MEASURED]
- consumer throughput: [TO BE MEASURED]
