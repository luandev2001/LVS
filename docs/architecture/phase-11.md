# Phase 11 — Async Fan-Out/Fan-In, Thread Pool, DB Pool & Backpressure

## Goal

Study asynchronous request processing without assuming that `CompletableFuture` automatically makes a system faster.

The lab compares:

1. sequential page reads
2. `CompletableFuture` fan-out/fan-in using a bounded executor
3. executor saturation and explicit backpressure
4. async worker concurrency versus Hikari DB connection-pool capacity

The key question is: **does async reduce wall-clock latency for independent I/O, or merely move the bottleneck from the servlet thread to another finite resource?**

No benchmark value is claimed until the service is actually run.

## Architecture

```text
HTTP request
    |
    +--> /async/sequential
    |       |
    |       +--> DB page 0 -> DB page 1 -> DB page 2 -> DB page 3
    |
    +--> /async
            |
            v
      bounded executor
       /    |    |    \
      DB0  DB1  DB2  DB3
       \    |    |    /
        CompletableFuture.allOf
                |
             response
```

The executor is deliberately bounded. It uses `AbortPolicy`, not `CallerRunsPolicy`, because `CallerRunsPolicy` can make the servlet/request thread execute overflow work and hide the actual saturation problem.

## Why CompletableFuture is used here

The endpoint returns a `CompletableFuture<ResponseEntity<...>>`. Spring MVC can release the servlet container thread while the asynchronous computation continues on the application executor.

That does **not** mean the DB work is non-blocking. Each worker still performs blocking JDBC I/O and consumes a DB connection while its transaction is active.

Therefore the resource chain is:

```text
HTTP concurrency
      ↓
async executor threads
      ↓
JDBC connections
      ↓
PostgreSQL
```

Increasing one layer without considering the next layer can simply move the queue.

## Bounded executor

Configuration:

```yaml
async:
  inventory:
    core-pool-size: 4
    max-pool-size: 8
    queue-capacity: 16
    keep-alive-seconds: 30
```

Environment variables:

- `INVENTORY_ASYNC_CORE_POOL_SIZE`
- `INVENTORY_ASYNC_MAX_POOL_SIZE`
- `INVENTORY_ASYNC_QUEUE_CAPACITY`
- `INVENTORY_ASYNC_KEEP_ALIVE_SECONDS`
- `INVENTORY_ASYNC_SIMULATED_DB_DELAY_MS`

When the executor and queue are full, new work is rejected and the API returns HTTP `429` with `ASYNC_BACKPRESSURE`.

This is intentional. Silent unlimited queuing is not free: it increases memory usage, request age and tail latency.

## DB connection-pool relationship

Inventory uses HikariCP:

```yaml
spring:
  datasource:
    hikari:
      maximum-pool-size: 10
      minimum-idle: 2
```

Each async page is executed through a separate Spring transactional DB-reader. This makes the experiment visible: multiple worker threads can concurrently hold multiple DB connections.

Examples to test:

```text
async max threads = 4, DB pool = 10
async max threads = 8, DB pool = 10
async max threads = 16, DB pool = 10
async max threads = 32, DB pool = 10
```

The final cases intentionally create more application concurrency than available DB connections. The expected observation is not a guaranteed latency number; measure whether Hikari pending/active connections become the limiting queue.

Then reverse the experiment:

```text
async max threads = 16, DB pool = 4
async max threads = 16, DB pool = 16
```

This isolates whether increasing DB capacity changes the bottleneck.

## Endpoints

### Sequential control

```http
GET /api/inventory/async/sequential?warehouseCode=WH-1&pages=4&size=50
```

### Async fan-out/fan-in

```http
GET /api/inventory/async?warehouseCode=WH-1&pages=4&size=50
```

The four page queries can execute concurrently, subject to the bounded executor and DB pool.

## Backpressure experiment

Run enough concurrent requests with enough pages to saturate the executor:

```bash
VUS=100 PAGES=8 k6 run load-tests/async-backpressure-phase11.js
```

Observe:

- HTTP 429 rate
- `inventory.async.rejected`
- executor active threads
- executor queue depth
- request p95/p99
- Hikari active/pending connections
- PostgreSQL CPU / active sessions

## Async versus sequential experiment

Sequential:

```bash
MODE=SEQUENTIAL VUS=20 PAGES=4 k6 run load-tests/async-phase11.js
```

Async:

```bash
MODE=ASYNC VUS=20 PAGES=4 k6 run load-tests/async-phase11.js
```

Keep dataset, page count, page size and simulated DB delay identical.

For a controlled I/O experiment:

```bash
INVENTORY_ASYNC_SIMULATED_DB_DELAY_MS=100
```

This delay is only an experimental knob. It must not be interpreted as a production DB benchmark.

## Important interpretation

If four independent queries each take approximately T milliseconds and there is enough executor and DB capacity, async fan-out can reduce wall-clock time toward the slowest individual query rather than the sum of all four.

But if the DB pool has fewer connections than concurrent tasks, tasks wait for connections. If PostgreSQL itself is saturated, adding more Java threads can increase contention without increasing useful throughput.

Therefore `CompletableFuture` is an orchestration mechanism, not a performance guarantee.

## Common anti-patterns demonstrated by this phase

### 1. Unbounded executor

```text
request -> submit unlimited work -> queue grows indefinitely
```

Symptoms:

- memory pressure
- old requests waiting behind newer work
- severe tail latency
- eventual failure amplification

### 2. CallerRunsPolicy as accidental backpressure

```text
executor full
    ↓
HTTP thread executes DB work itself
```

This can protect the queue but also consumes servlet threads and destroys the intended isolation between request processing and background work.

### 3. Too many async workers

```text
32 workers
10 DB connections
```

The extra 22 workers do not create extra PostgreSQL connections. They can simply spend time waiting for the pool.

### 4. Holding DB transactions across unrelated async work

Do not keep a transaction open while waiting for unrelated `CompletableFuture`s. Transaction scope should normally stay close to the DB operation that needs it.

## Metrics

The phase exposes:

- `inventory.async.rejected`
- `inventory.async.fanout`
- `inventory.async.pool.active`
- `inventory.async.pool.size`
- `inventory.async.pool.queue`

Also inspect Hikari metrics exposed by Actuator and PostgreSQL connection/activity metrics.

## Alternatives

| Approach | Phase 11 treatment |
|---|---|
| `CompletableFuture` | Chosen for explicit fan-out/fan-in and interview-level understanding of Java async composition |
| Spring `@Async` | Not primary because executor selection and composition are less explicit in the lab's orchestration path |
| Reactor/WebFlux | Reserved for a later comparison; reactive DB drivers and end-to-end non-blocking I/O change the model significantly |
| Virtual threads | Important Java 21 alternative; should be compared separately because it changes the cost model for blocking I/O but does not remove DB connection limits |
| Unbounded executor | Deliberately rejected because overload becomes queue/memory growth |

## Measurements

Record for every scenario:

- p50/p95/p99 latency
- throughput
- error/429 rate
- async executor active/queue/rejected
- Hikari active/idle/pending/max
- PostgreSQL active sessions and CPU
- JVM CPU and memory

Use `[TO BE MEASURED]` until actual runs are performed.
