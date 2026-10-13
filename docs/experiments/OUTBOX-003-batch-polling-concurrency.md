# OUTBOX-003 — Polling Interval, Batch Size, and Publisher Concurrency

## Objective

Measure how Outbox polling and publisher parallelism affect latency and throughput.

## Variables

### Polling interval

```text
OUTBOX_POLLING_MS
```

Suggested experimental values:

```text
100
500
1000
```

### Batch size

```text
OUTBOX_BATCH_SIZE
```

Suggested values:

```text
10
50
200
```

### Publisher concurrency

```text
OUTBOX_PUBLISHER_CONCURRENCY
```

Suggested values:

```text
1
2
4
```

These are experiment inputs, not claims that any one value is optimal.

## Workload

Use:

```bash
k6 run load-tests/payment-outbox.js
```

Run the same workload for every configuration.

## Metrics

Capture:

- HTTP request latency
- payment creation throughput
- outbox insertion latency
- pending backlog
- oldest pending event age
- publication throughput
- Kafka producer throughput
- consumer lag
- DB CPU
- DB connections
- JVM CPU
- JVM memory

## Methodology

For each configuration:

1. reset/clean the test database if required;
2. warm up;
3. run the same workload;
4. collect metrics;
5. record outbox backlog and lag;
6. compare with the same baseline.

Do not compare runs with different workload sizes or infrastructure.

## Result table

| Polling ms | Batch | Concurrency | Throughput | Publish latency | Max backlog | DB CPU | Kafka lag |
|---:|---:|---:|---:|---:|---:|---:|---:|
| 100 | 10 | 1 | [TO BE MEASURED] | [TO BE MEASURED] | [TO BE MEASURED] | [TO BE MEASURED] | [TO BE MEASURED] |
| 500 | 50 | 2 | [TO BE MEASURED] | [TO BE MEASURED] | [TO BE MEASURED] | [TO BE MEASURED] | [TO BE MEASURED] |
| 1000 | 200 | 4 | [TO BE MEASURED] | [TO BE MEASURED] | [TO BE MEASURED] | [TO BE MEASURED] | [TO BE MEASURED] |

The values above are placeholders only.
